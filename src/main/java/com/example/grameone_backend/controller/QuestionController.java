package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.AnswerOption;
import com.example.grameone_backend.entity.Concept;
import com.example.grameone_backend.entity.Question;
import com.example.grameone_backend.repository.ConceptRepository;
import com.example.grameone_backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final ConceptRepository conceptRepository;

    @GetMapping
    public List<Question> getQuestions(
            @RequestParam(required = false) Long conceptId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) String status) {
        if (conceptId != null) {
            return questionRepository.findByConceptId(conceptId);
        }
        if (gradeId != null && status != null) {
            return questionRepository.findByGradeIdAndStatus(gradeId, status);
        }
        return questionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        return questionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestParam Long conceptId, @RequestBody Question question) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new RuntimeException("Concept not found"));
        question.setConcept(concept);
        if (question.getAnswerOptions() != null) {
            for (AnswerOption option : question.getAnswerOptions()) {
                option.setQuestion(question);
            }
        }
        return ResponseEntity.ok(questionRepository.save(question));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question details) {
        return questionRepository.findById(id)
                .map(existing -> {
                    existing.setQuestionText(details.getQuestionText());
                    existing.setQuestionType(details.getQuestionType());
                    existing.setDifficulty(details.getDifficulty());
                    existing.setExplanation(details.getExplanation());
                    existing.setComprehensionText(details.getComprehensionText());
                    existing.setImageUrl(details.getImageUrl());
                    existing.setDiagramUrl(details.getDiagramUrl());
                    existing.setStatus(details.getStatus());

                    if (details.getAnswerOptions() != null) {
                        existing.getAnswerOptions().clear();
                        for (AnswerOption option : details.getAnswerOptions()) {
                            option.setQuestion(existing);
                            existing.getAnswerOptions().add(option);
                        }
                    }
                    return ResponseEntity.ok(questionRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/upload-media")
    public ResponseEntity<?> uploadQuestionMedia(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type,
            com.example.grameone_backend.service.MediaService mediaService) {

        return questionRepository.findById(id)
                .map(question -> {
                    try {
                        String key = mediaService.uploadMedia(file, "questions");
                        String publicUrl = mediaService.getPublicUrl(key);
                        if ("diagram".equalsIgnoreCase(type)) {
                            question.setDiagramUrl(publicUrl);
                        } else {
                            question.setImageUrl(publicUrl);
                        }
                        return ResponseEntity.ok(questionRepository.save(question));
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().body("Failed to upload image: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        if (questionRepository.existsById(id)) {
            questionRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
