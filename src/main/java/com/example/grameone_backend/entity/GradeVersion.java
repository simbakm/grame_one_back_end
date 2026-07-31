package com.example.grameone_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grade_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;

    @Column(nullable = false)
    private String gradeName;

    @Column(nullable = false)
    private String version; // e.g. "1.0", "1.1", "2.0"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String packageR2Url; // Full R2 package download URL or object path

    private Long packageSizeBytes;

    private String checksumSha256;

    @Column(columnDefinition = "TEXT")
    private String changelog;

    @Column(nullable = false)
    private Boolean isLatest;

    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
        if (this.isLatest == null) {
            this.isLatest = true;
        }
    }
}
