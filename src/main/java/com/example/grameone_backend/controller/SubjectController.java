package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.Grade;
import com.example.grameone_backend.entity.Subject;
import com.example.grameone_backend.repository.GradeRepository;
import com.example.grameone_backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;

    @GetMapping
    public List<Subject> getSubjects(@RequestParam(required = false) Long gradeId) {
        if (gradeId != null) {
            return subjectRepository.findByGradeId(gradeId);
        }
        return subjectRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subject> getSubjectById(@PathVariable Long id) {
        return subjectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createSubject(@RequestParam Long gradeId, @RequestBody Subject subject) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Grade not found"));
        subject.setGrade(grade);
        return ResponseEntity.ok(subjectRepository.save(subject));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Subject> updateSubject(@PathVariable Long id, @RequestBody Subject details) {
        return subjectRepository.findById(id)
                .map(existing -> {
                    existing.setName(details.getName());
                    existing.setCode(details.getCode());
                    existing.setDescription(details.getDescription());
                    existing.setLanguage(details.getLanguage());
                    return ResponseEntity.ok(subjectRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        if (subjectRepository.existsById(id)) {
            subjectRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
