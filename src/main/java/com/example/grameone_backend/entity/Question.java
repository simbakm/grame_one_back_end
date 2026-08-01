package com.example.grameone_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer questionNumber; // Sequential number per grade/subject e.g. 1, 2, 3...

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    private String questionType; // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER

    private String difficulty; // EASY, MEDIUM, HARD

    @Column(columnDefinition = "TEXT")
    private String explanation; // General explanation for the question

    @Column(columnDefinition = "TEXT")
    private String comprehensionText; // Optional reading comprehension passage or story

    private String imageUrl; // Cloudflare R2 object path or URL for image

    private String diagramUrl; // Cloudflare R2 object path or URL for diagram

    private String status; // DRAFT, APPROVED

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnswerOption> answerOptions = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("questionCode")
    public String getQuestionCode() {
        try {
            if (concept != null && concept.getUnit() != null && concept.getUnit().getTopic() != null &&
                    concept.getUnit().getTopic().getSubject() != null && concept.getUnit().getTopic().getSubject().getGrade() != null) {

                Grade grade = concept.getUnit().getTopic().getSubject().getGrade();
                Subject subject = concept.getUnit().getTopic().getSubject();

                String gradeDigit = extractGradeDigit(grade.getName(), grade.getCode());
                String subjectInitial = extractSubjectInitial(subject.getName());
                int qNum = (questionNumber != null && questionNumber > 0) ? questionNumber : (id != null ? id.intValue() : 1);
                String numFormatted = String.format("%02d", qNum);

                return gradeDigit + subjectInitial + numFormatted;
            }
        } catch (Exception ignored) {}
        int fallbackNum = (questionNumber != null && questionNumber > 0) ? questionNumber : (id != null ? id.intValue() : 1);
        return "7M" + String.format("%02d", fallbackNum);
    }

    private String extractGradeDigit(String name, String code) {
        if (name != null) {
            String digits = name.replaceAll("\\D+", "");
            if (!digits.isEmpty()) return digits;
        }
        if (code != null) {
            String digits = code.replaceAll("\\D+", "");
            if (!digits.isEmpty()) return digits;
        }
        return "7";
    }

    private String extractSubjectInitial(String name) {
        if (name == null || name.trim().isEmpty()) return "M";
        String clean = name.trim().toUpperCase();
        if (clean.startsWith("MATH")) return "M";
        if (clean.startsWith("SCI")) return "S";
        if (clean.startsWith("SOC")) return "SS";
        if (clean.startsWith("CHI") || clean.startsWith("SHO")) return "SH";
        if (clean.startsWith("NDE")) return "N";
        return String.valueOf(clean.charAt(0));
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "APPROVED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
