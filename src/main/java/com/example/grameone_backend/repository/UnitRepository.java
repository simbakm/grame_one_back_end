package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByTopicId(Long topicId);
    Optional<Unit> findByNameIgnoreCaseAndTopicId(String name, Long topicId);
}
