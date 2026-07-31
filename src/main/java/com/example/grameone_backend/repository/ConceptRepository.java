package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConceptRepository extends JpaRepository<Concept, Long> {
    List<Concept> findByUnitId(Long unitId);
    Optional<Concept> findByNameIgnoreCaseAndUnitId(String name, Long unitId);
}
