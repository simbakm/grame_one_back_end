package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findByNameIgnoreCase(String name);
    Optional<Grade> findByCodeIgnoreCase(String code);
}
