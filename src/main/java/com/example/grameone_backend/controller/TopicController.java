package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.Subject;
import com.example.grameone_backend.entity.Topic;
import com.example.grameone_backend.repository.SubjectRepository;
import com.example.grameone_backend.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;

    @GetMapping
    public List<Topic> getTopics(@RequestParam(required = false) Long subjectId) {
        if (subjectId != null) {
            return topicRepository.findBySubjectId(subjectId);
        }
        return topicRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Topic> getTopicById(@PathVariable Long id) {
        return topicRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTopic(@RequestParam Long subjectId, @RequestBody Topic topic) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        topic.setSubject(subject);
        return ResponseEntity.ok(topicRepository.save(topic));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Topic> updateTopic(@PathVariable Long id, @RequestBody Topic details) {
        return topicRepository.findById(id)
                .map(existing -> {
                    existing.setName(details.getName());
                    existing.setCode(details.getCode());
                    existing.setTopicNumber(details.getTopicNumber());
                    existing.setDescription(details.getDescription());
                    return ResponseEntity.ok(topicRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        if (topicRepository.existsById(id)) {
            topicRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
