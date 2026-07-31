package com.example.grameone_backend.service;

import com.example.grameone_backend.entity.*;
import com.example.grameone_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UnitRepository unitRepository;
    private final ConceptRepository conceptRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public Map<String, Object> importQuestionsFromCsv(MultipartFile file) throws Exception {
        int importedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        // In-memory caches to avoid repeated DB lookups within the same import batch
        Map<String, Grade>   gradeCache   = new HashMap<>();
        Map<String, Subject> subjectCache = new HashMap<>();
        Map<String, Topic>   topicCache   = new HashMap<>();
        Map<String, Unit>    unitCache    = new HashMap<>();
        Map<String, Concept> conceptCache = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : csvParser) {
                try {
                    String gradeName   = getVal(record, "Grade");
                    String subjectName = getVal(record, "Subject");
                    String topicName   = getVal(record, "Topic");
                    String unitName    = getVal(record, "Unit");
                    String conceptName = getVal(record, "Concept");
                    String questionText = getVal(record, "QuestionText");

                    if (gradeName.isEmpty() || subjectName.isEmpty() || questionText.isEmpty()) {
                        skippedCount++;
                        errors.add("Row " + record.getRecordNumber() + ": Missing required fields (Grade, Subject, QuestionText).");
                        continue;
                    }

                    // ── 1. Grade ──────────────────────────────────────────────────
                    String gradeKey = gradeName.toLowerCase();
                    Grade grade = gradeCache.computeIfAbsent(gradeKey, k ->
                            gradeRepository.findByNameIgnoreCase(gradeName)
                                    .orElseGet(() -> {
                                        log.info("[CSV Import] Creating new Grade: {}", gradeName);
                                        return gradeRepository.save(Grade.builder()
                                                .name(gradeName)
                                                .code(gradeName.toUpperCase().replace(" ", "_"))
                                                .sortOrder(extractGradeNumber(gradeName))
                                                .description(gradeName + " Curriculum")
                                                .build());
                                    })
                    );

                    // ── 2. Subject ────────────────────────────────────────────────
                    String subjectKey = (gradeName + "||" + subjectName).toLowerCase();
                    Subject subject = subjectCache.computeIfAbsent(subjectKey, k ->
                            subjectRepository.findByNameIgnoreCaseAndGradeId(subjectName, grade.getId())
                                    .orElseGet(() -> {
                                        log.info("[CSV Import] Creating new Subject: {} under {}", subjectName, gradeName);
                                        return subjectRepository.save(Subject.builder()
                                                .name(subjectName)
                                                .code(subjectName.toUpperCase().replace(" ", "_") + "_" + grade.getId())
                                                .language("English")
                                                .grade(grade)
                                                .build());
                                    })
                    );

                    // ── 3. Topic ──────────────────────────────────────────────────
                    if (topicName.isEmpty()) topicName = "General";
                    String topicKey = (subjectKey + "||" + topicName).toLowerCase();
                    final String finalTopicName = topicName;
                    final Subject finalSubject  = subject;
                    Topic topic = topicCache.computeIfAbsent(topicKey, k ->
                            topicRepository.findByNameIgnoreCaseAndSubjectId(finalTopicName, finalSubject.getId())
                                    .orElseGet(() -> {
                                        log.info("[CSV Import] Creating new Topic: {} under {}", finalTopicName, finalSubject.getName());
                                        return topicRepository.save(Topic.builder()
                                                .name(finalTopicName)
                                                .subject(finalSubject)
                                                .build());
                                    })
                    );

                    // ── 4. Unit ───────────────────────────────────────────────────
                    if (unitName.isEmpty()) unitName = "General Unit";
                    String unitKey = (topicKey + "||" + unitName).toLowerCase();
                    final String finalUnitName = unitName;
                    final Topic  finalTopic    = topic;
                    Unit unit = unitCache.computeIfAbsent(unitKey, k ->
                            unitRepository.findByNameIgnoreCaseAndTopicId(finalUnitName, finalTopic.getId())
                                    .orElseGet(() -> {
                                        log.info("[CSV Import] Creating new Unit: {} under {}", finalUnitName, finalTopic.getName());
                                        return unitRepository.save(Unit.builder()
                                                .name(finalUnitName)
                                                .topic(finalTopic)
                                                .build());
                                    })
                    );

                    // ── 5. Concept ────────────────────────────────────────────────
                    if (conceptName.isEmpty()) conceptName = "General Concept";
                    String conceptKey = (unitKey + "||" + conceptName).toLowerCase();
                    final String finalConceptName = conceptName;
                    final Unit   finalUnit        = unit;
                    Concept concept = conceptCache.computeIfAbsent(conceptKey, k ->
                            conceptRepository.findByNameIgnoreCaseAndUnitId(finalConceptName, finalUnit.getId())
                                    .orElseGet(() -> {
                                        log.info("[CSV Import] Creating new Concept: {} under {}", finalConceptName, finalUnit.getName());
                                        return conceptRepository.save(Concept.builder()
                                                .name(finalConceptName)
                                                .unit(finalUnit)
                                                .build());
                                    })
                    );

                    // ── 6. Question & Answer Options ──────────────────────────────
                    String questionNumStr = getVal(record, "QuestionNumber");
                    int qNumber = 1;
                    if (!questionNumStr.isEmpty()) {
                        try { qNumber = Integer.parseInt(questionNumStr.replaceAll("\\D+", "")); } catch (Exception ignored) {}
                    } else {
                        qNumber = importedCount + 1;
                    }

                    // Build question code: e.g. 7M01
                    String gradeDigit   = String.valueOf(extractGradeNumber(gradeName));
                    String subjectInit  = subjectName.trim().substring(0, 1).toUpperCase();
                    String qCode        = gradeDigit + subjectInit + String.format("%02d", qNumber);

                    String optionA      = getVal(record, "OptionA");
                    String optionB      = getVal(record, "OptionB");
                    String optionC      = getVal(record, "OptionC");
                    String optionD      = getVal(record, "OptionD");
                    String correctOpt   = getVal(record, "CorrectOption").trim().toUpperCase();
                    String explanation  = getVal(record, "Explanation");
                    String difficulty   = getVal(record, "Difficulty");
                    if (difficulty.isEmpty()) difficulty = "MEDIUM";

                    Question question = Question.builder()
                            .questionNumber(qNumber)
                            .questionText(questionText)
                            .questionType("MULTIPLE_CHOICE")
                            .difficulty(difficulty.toUpperCase())
                            .explanation(explanation)
                            .status("APPROVED")
                            .concept(concept)
                            .build();

                    List<AnswerOption> options = new ArrayList<>();
                    if (!optionA.isEmpty()) options.add(makeOption(optionA, "A".equals(correctOpt) || "OPTIONA".equals(correctOpt), question, 1));
                    if (!optionB.isEmpty()) options.add(makeOption(optionB, "B".equals(correctOpt) || "OPTIONB".equals(correctOpt), question, 2));
                    if (!optionC.isEmpty()) options.add(makeOption(optionC, "C".equals(correctOpt) || "OPTIONC".equals(correctOpt), question, 3));
                    if (!optionD.isEmpty()) options.add(makeOption(optionD, "D".equals(correctOpt) || "OPTIOND".equals(correctOpt), question, 4));

                    question.setAnswerOptions(options);
                    questionRepository.save(question);
                    importedCount++;
                    log.info("[CSV Import] Saved question #{} [{}]: {}", qNumber, qCode, questionText.substring(0, Math.min(40, questionText.length())));

                } catch (Exception ex) {
                    skippedCount++;
                    String msg = "Row " + record.getRecordNumber() + ": " + ex.getMessage();
                    errors.add(msg);
                    log.error("[CSV Import] {}", msg, ex);
                }
            }
        }

        log.info("[CSV Import] Complete — Imported: {}, Skipped: {}, Errors: {}", importedCount, skippedCount, errors.size());

        Map<String, Object> result = new HashMap<>();
        result.put("imported", importedCount);
        result.put("skipped", skippedCount);
        result.put("errors", errors);
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AnswerOption makeOption(String text, boolean correct, Question question, int order) {
        return AnswerOption.builder()
                .optionText(text)
                .isCorrect(correct)
                .question(question)
                .sortOrder(order)
                .build();
    }

    private String getVal(CSVRecord record, String header) {
        if (record.isMapped(header)) {
            String val = record.get(header);
            return val != null ? val.trim() : "";
        }
        return "";
    }

    private int extractGradeNumber(String gradeName) {
        try {
            String digits = gradeName.replaceAll("\\D+", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }
}
