package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.GradeVersion;
import com.example.grameone_backend.service.PackageBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageBuilderService packageBuilderService;

    @PostMapping("/publish/{gradeId}")
    public ResponseEntity<?> publishGradePackage(
            @PathVariable Long gradeId,
            @RequestParam(required = false, defaultValue = "Published content package update") String changelog) {
        try {
            GradeVersion version = packageBuilderService.publishGradePackage(gradeId, changelog);
            return ResponseEntity.ok(version);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to publish grade package: " + e.getMessage());
        }
    }

    @GetMapping("/history/{gradeId}")
    public List<GradeVersion> getVersionHistory(@PathVariable Long gradeId) {
        return packageBuilderService.getVersionHistory(gradeId);
    }

    @PostMapping("/rollback/{gradeId}")
    public ResponseEntity<?> rollbackVersion(
            @PathVariable Long gradeId,
            @RequestParam String version) {
        try {
            GradeVersion rolledBack = packageBuilderService.rollbackVersion(gradeId, version);
            return ResponseEntity.ok(rolledBack);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Rollback failed: " + e.getMessage());
        }
    }
}
