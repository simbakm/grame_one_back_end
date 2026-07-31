package com.example.grameone_backend.service;

import com.example.grameone_backend.entity.*;
import com.example.grameone_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SqliteGeneratorService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UnitRepository unitRepository;
    private final ConceptRepository conceptRepository;
    private final QuestionRepository questionRepository;

    public File generateSqliteDatabaseForGrade(Long gradeId, File targetFile) throws Exception {
        if (targetFile.exists()) {
            targetFile.delete();
        }

        String jdbcUrl = "jdbc:sqlite:" + targetFile.getAbsolutePath();

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                // 1. Create Tables matching mobile offline schema
                stmt.execute("CREATE TABLE subjects (" +
                        "id INTEGER PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "code TEXT, " +
                        "description TEXT, " +
                        "language TEXT);");

                stmt.execute("CREATE TABLE topics (" +
                        "id INTEGER PRIMARY KEY, " +
                        "subject_id INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "code TEXT, " +
                        "topic_number INTEGER, " +
                        "description TEXT, " +
                        "FOREIGN KEY(subject_id) REFERENCES subjects(id));");

                stmt.execute("CREATE TABLE units (" +
                        "id INTEGER PRIMARY KEY, " +
                        "topic_id INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "code TEXT, " +
                        "unit_number INTEGER, " +
                        "description TEXT, " +
                        "FOREIGN KEY(topic_id) REFERENCES topics(id));");

                stmt.execute("CREATE TABLE concepts (" +
                        "id INTEGER PRIMARY KEY, " +
                        "unit_id INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "code TEXT, " +
                        "summary TEXT, " +
                        "key_takeaways TEXT, " +
                        "FOREIGN KEY(unit_id) REFERENCES units(id));");

                stmt.execute("CREATE TABLE questions (" +
                        "id INTEGER PRIMARY KEY, " +
                        "concept_id INTEGER NOT NULL, " +
                        "question_text TEXT NOT NULL, " +
                        "question_type TEXT, " +
                        "difficulty TEXT, " +
                        "explanation TEXT, " +
                        "image_url TEXT, " +
                        "diagram_url TEXT, " +
                        "FOREIGN KEY(concept_id) REFERENCES concepts(id));");

                stmt.execute("CREATE TABLE answer_options (" +
                        "id INTEGER PRIMARY KEY, " +
                        "question_id INTEGER NOT NULL, " +
                        "option_text TEXT NOT NULL, " +
                        "is_correct INTEGER NOT NULL, " +
                        "explanation TEXT, " +
                        "sort_order INTEGER, " +
                        "FOREIGN KEY(question_id) REFERENCES questions(id));");
            }

            // 2. Export Subjects
            List<Subject> subjects = subjectRepository.findByGradeId(gradeId);
            String insertSubjectSql = "INSERT INTO subjects (id, name, code, description, language) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSubjectSql)) {
                for (Subject subject : subjects) {
                    pstmt.setLong(1, subject.getId());
                    pstmt.setString(2, subject.getName());
                    pstmt.setString(3, subject.getCode());
                    pstmt.setString(4, subject.getDescription());
                    pstmt.setString(5, subject.getLanguage());
                    pstmt.addBatch();

                    // Export Topics
                    List<Topic> topics = topicRepository.findBySubjectId(subject.getId());
                    String insertTopicSql = "INSERT INTO topics (id, subject_id, name, code, topic_number, description) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement topicStmt = conn.prepareStatement(insertTopicSql)) {
                        for (Topic topic : topics) {
                            topicStmt.setLong(1, topic.getId());
                            topicStmt.setLong(2, subject.getId());
                            topicStmt.setString(3, topic.getName());
                            topicStmt.setString(4, topic.getCode());
                            topicStmt.setObject(5, topic.getTopicNumber());
                            topicStmt.setString(6, topic.getDescription());
                            topicStmt.addBatch();

                            // Export Units
                            List<Unit> units = unitRepository.findByTopicId(topic.getId());
                            String insertUnitSql = "INSERT INTO units (id, topic_id, name, code, unit_number, description) VALUES (?, ?, ?, ?, ?, ?)";
                            try (PreparedStatement unitStmt = conn.prepareStatement(insertUnitSql)) {
                                for (Unit unit : units) {
                                    unitStmt.setLong(1, unit.getId());
                                    unitStmt.setLong(2, topic.getId());
                                    unitStmt.setString(3, unit.getName());
                                    unitStmt.setString(4, unit.getCode());
                                    unitStmt.setObject(5, unit.getUnitNumber());
                                    unitStmt.setString(6, unit.getDescription());
                                    unitStmt.addBatch();

                                    // Export Concepts
                                    List<Concept> concepts = conceptRepository.findByUnitId(unit.getId());
                                    String insertConceptSql = "INSERT INTO concepts (id, unit_id, name, code, summary, key_takeaways) VALUES (?, ?, ?, ?, ?, ?)";
                                    try (PreparedStatement conceptStmt = conn.prepareStatement(insertConceptSql)) {
                                        for (Concept concept : concepts) {
                                            conceptStmt.setLong(1, concept.getId());
                                            conceptStmt.setLong(2, unit.getId());
                                            conceptStmt.setString(3, concept.getName());
                                            conceptStmt.setString(4, concept.getCode());
                                            conceptStmt.setString(5, concept.getSummary());
                                            conceptStmt.setString(6, concept.getKeyTakeaways());
                                            conceptStmt.addBatch();

                                            // Export Questions
                                            List<Question> questions = questionRepository.findByConceptId(concept.getId());
                                            String insertQuestionSql = "INSERT INTO questions (id, concept_id, question_text, question_type, difficulty, explanation, image_url, diagram_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                                            try (PreparedStatement qStmt = conn.prepareStatement(insertQuestionSql)) {
                                                for (Question q : questions) {
                                                    if (!"APPROVED".equalsIgnoreCase(q.getStatus())) continue;

                                                    qStmt.setLong(1, q.getId());
                                                    qStmt.setLong(2, concept.getId());
                                                    qStmt.setString(3, q.getQuestionText());
                                                    qStmt.setString(4, q.getQuestionType());
                                                    qStmt.setString(5, q.getDifficulty());
                                                    qStmt.setString(6, q.getExplanation());
                                                    qStmt.setString(7, q.getImageUrl());
                                                    qStmt.setString(8, q.getDiagramUrl());
                                                    qStmt.addBatch();

                                                    // Export Answer Options
                                                    if (q.getAnswerOptions() != null) {
                                                        String insertOptSql = "INSERT INTO answer_options (id, question_id, option_text, is_correct, explanation, sort_order) VALUES (?, ?, ?, ?, ?, ?)";
                                                        try (PreparedStatement optStmt = conn.prepareStatement(insertOptSql)) {
                                                            for (AnswerOption opt : q.getAnswerOptions()) {
                                                                optStmt.setLong(1, opt.getId());
                                                                optStmt.setLong(2, q.getId());
                                                                optStmt.setString(3, opt.getOptionText());
                                                                optStmt.setInt(4, Boolean.TRUE.equals(opt.getIsCorrect()) ? 1 : 0);
                                                                optStmt.setString(5, opt.getExplanation());
                                                                optStmt.setObject(6, opt.getSortOrder());
                                                                optStmt.addBatch();
                                                            }
                                                            optStmt.executeBatch();
                                                        }
                                                    }
                                                }
                                                qStmt.executeBatch();
                                            }
                                        }
                                        conceptStmt.executeBatch();
                                    }
                                }
                                unitStmt.executeBatch();
                            }
                        }
                        topicStmt.executeBatch();
                    }
                }
                pstmt.executeBatch();
            }

            conn.commit();
        }

        return targetFile;
    }
}
