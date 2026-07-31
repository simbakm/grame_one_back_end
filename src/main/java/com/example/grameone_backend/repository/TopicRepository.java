package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySubjectId(Long subjectId);
    Optional<Topic> findByNameIgnoreCaseAndSubjectId(String name, Long subjectId);
}
