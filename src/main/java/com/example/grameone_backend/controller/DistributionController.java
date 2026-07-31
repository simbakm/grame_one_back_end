package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.GradeVersion;
import com.example.grameone_backend.repository.GradeVersionRepository;
import com.example.grameone_backend.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/distribution")
@RequiredArgsConstructor
public class DistributionController {

    private final GradeVersionRepository gradeVersionRepository;
    private final LicenseService licenseService;

    @GetMapping("/grades/{gradeId}/latest")
    public ResponseEntity<?> getLatestGradePackage(
            @PathVariable Long gradeId,
            @RequestParam(required = false) String licenseCode,
            @RequestParam(required = false) String deviceId) {

        // 1. Validate License if provided
        if (licenseCode != null && deviceId != null) {
            Map<String, Object> licenseResult = licenseService.validateAndActivateLicense(licenseCode, deviceId);
            Boolean isValid = (Boolean) licenseResult.get("valid");
            if (!Boolean.TRUE.equals(isValid)) {
                return ResponseEntity.status(403).body(licenseResult);
            }
        }

        // 2. Query latest published package version for grade
        Optional<GradeVersion> latestOpt = gradeVersionRepository.findByGradeIdAndIsLatestTrue(gradeId);
        if (latestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        GradeVersion latest = latestOpt.get();

        Map<String, Object> response = new HashMap<>();
        response.put("gradeId", latest.getGrade().getId());
        response.put("gradeName", latest.getGradeName());
        response.put("version", latest.getVersion());
        response.put("packageUrl", latest.getPackageR2Url());
        response.put("packageSizeBytes", latest.getPackageSizeBytes());
        response.put("checksumSha256", latest.getChecksumSha256());
        response.put("publishedAt", latest.getPublishedAt());
        response.put("changelog", latest.getChangelog());

        return ResponseEntity.ok(response);
    }
}
