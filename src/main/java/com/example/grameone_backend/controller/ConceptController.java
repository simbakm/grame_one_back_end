package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.Concept;
import com.example.grameone_backend.entity.Unit;
import com.example.grameone_backend.repository.ConceptRepository;
import com.example.grameone_backend.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/concepts")
@RequiredArgsConstructor
public class ConceptController {

    private final ConceptRepository conceptRepository;
    private final UnitRepository unitRepository;

    @GetMapping
    public List<Concept> getConcepts(@RequestParam(required = false) Long unitId) {
        if (unitId != null) {
            return conceptRepository.findByUnitId(unitId);
        }
        return conceptRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Concept> getConceptById(@PathVariable Long id) {
        return conceptRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createConcept(@RequestParam Long unitId, @RequestBody Concept concept) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));
        concept.setUnit(unit);
        return ResponseEntity.ok(conceptRepository.save(concept));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Concept> updateConcept(@PathVariable Long id, @RequestBody Concept details) {
        return conceptRepository.findById(id)
                .map(existing -> {
                    existing.setName(details.getName());
                    existing.setCode(details.getCode());
                    existing.setSummary(details.getSummary());
                    existing.setKeyTakeaways(details.getKeyTakeaways());
                    return ResponseEntity.ok(conceptRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConcept(@PathVariable Long id) {
        if (conceptRepository.existsById(id)) {
            conceptRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
