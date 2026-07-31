package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.Topic;
import com.example.grameone_backend.entity.Unit;
import com.example.grameone_backend.repository.TopicRepository;
import com.example.grameone_backend.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitRepository unitRepository;
    private final TopicRepository topicRepository;

    @GetMapping
    public List<Unit> getUnits(@RequestParam(required = false) Long topicId) {
        if (topicId != null) {
            return unitRepository.findByTopicId(topicId);
        }
        return unitRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Unit> getUnitById(@PathVariable Long id) {
        return unitRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUnit(@RequestParam Long topicId, @RequestBody Unit unit) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));
        unit.setTopic(topic);
        return ResponseEntity.ok(unitRepository.save(unit));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Unit> updateUnit(@PathVariable Long id, @RequestBody Unit details) {
        return unitRepository.findById(id)
                .map(existing -> {
                    existing.setName(details.getName());
                    existing.setCode(details.getCode());
                    existing.setUnitNumber(details.getUnitNumber());
                    existing.setDescription(details.getDescription());
                    return ResponseEntity.ok(unitRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        if (unitRepository.existsById(id)) {
            unitRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
