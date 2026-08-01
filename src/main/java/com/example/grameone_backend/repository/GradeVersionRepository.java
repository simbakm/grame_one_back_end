package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.GradeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GradeVersionRepository extends JpaRepository<GradeVersion, Long> {
    List<GradeVersion> findByGradeIdOrderByPublishedAtDesc(Long gradeId);
    Optional<GradeVersion> findByGradeIdAndIsLatestTrue(Long gradeId);
    Optional<GradeVersion> findByGradeIdAndVersion(Long gradeId, String version);
    long countByIsLatestTrue();
}
