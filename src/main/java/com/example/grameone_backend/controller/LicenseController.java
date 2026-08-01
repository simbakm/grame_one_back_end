package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.License;
import com.example.grameone_backend.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/licenses")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @GetMapping
    public List<License> getAllLicenses() {
        return licenseService.getAllLicenses();
    }

    @PostMapping("/generate")
    public License generateLicense(
            @RequestParam(value = "durationMonths", required = false, defaultValue = "12") Integer durationMonths,
            @RequestParam(value = "licenseType", required = false, defaultValue = "ORDINARY") String licenseType,
            @RequestParam(value = "validUntil", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validUntil) {
        return licenseService.generateLicense(durationMonths, licenseType, validUntil);
    }

    @PostMapping("/generate-bulk")
    public List<License> generateBulkLicenses(
            @RequestParam(value = "count", defaultValue = "10") int count,
            @RequestParam(value = "durationMonths", required = false, defaultValue = "12") Integer durationMonths,
            @RequestParam(value = "licenseType", required = false, defaultValue = "ORDINARY") String licenseType,
            @RequestParam(value = "validUntil", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validUntil) {
        return licenseService.generateBulkLicenses(count, durationMonths, licenseType, validUntil);
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateLicense(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String activationCode,
            @RequestParam(required = false) String deviceId) {

        String code = activationCode;
        String device = deviceId;

        if (body != null) {
            if (body.containsKey("activationCode") && body.get("activationCode") != null) {
                code = body.get("activationCode").toString();
            }
            if (body.containsKey("deviceId") && body.get("deviceId") != null) {
                device = body.get("deviceId").toString();
            }
        }

        if (code == null || code.trim().isEmpty() || device == null || device.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                "message", "Both activationCode and deviceId are required."
            ));
        }

        Map<String, Object> result = licenseService.validateAndActivateLicense(code.trim(), device.trim());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-multi")
    public ResponseEntity<?> validateMultiLicenses(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> codes = (List<String>) body.get("activationCodes");
        String deviceId = (String) body.get("deviceId");
        List<Map<String, Object>> results = licenseService.validateAndActivateMultiLicenses(codes, deviceId);
        return ResponseEntity.ok(results);
    }
}
