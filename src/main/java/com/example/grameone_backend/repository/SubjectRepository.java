package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByGradeId(Long gradeId);
    Optional<Subject> findByNameIgnoreCaseAndGradeId(String name, Long gradeId);
}
