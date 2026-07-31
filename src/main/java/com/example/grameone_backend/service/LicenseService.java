package com.example.grameone_backend.service;

import com.example.grameone_backend.entity.License;
import com.example.grameone_backend.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseRepository licenseRepository;

    public License generateLicense(Integer durationMonths, String licenseType, LocalDateTime validUntil) {
        String code = "GRAME-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase() +
                "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        String type = (licenseType != null && !licenseType.isEmpty()) ? licenseType.toUpperCase() : "ORDINARY";

        License license = License.builder()
                .activationCode(code)
                .licenseType(type)
                .subscriptionDurationMonths("FREE".equalsIgnoreCase(type) ? null : (durationMonths != null ? durationMonths : 12))
                .validUntil("FREE".equalsIgnoreCase(type) ? validUntil : null)
                .status("PENDING")
                .build();

        return licenseRepository.save(license);
    }

    public List<License> generateBulkLicenses(int count, Integer durationMonths, String licenseType, LocalDateTime validUntil) {
        List<License> result = new ArrayList<>();
        int safeCount = Math.max(1, Math.min(count, 500)); // cap max 500 at once
        for (int i = 0; i < safeCount; i++) {
            result.add(generateLicense(durationMonths, licenseType, validUntil));
        }
        return result;
    }

    public List<License> getAllLicenses() {
        return licenseRepository.findAll();
    }

    public Map<String, Object> validateAndActivateLicense(String activationCode, String deviceId) {
        Map<String, Object> response = new HashMap<>();

        Optional<License> optionalLicense = licenseRepository.findByActivationCode(activationCode);
        if (optionalLicense.isEmpty()) {
            response.put("valid", false);
            response.put("message", "Invalid activation code");
            return response;
        }

        License license = optionalLicense.get();

        if ("REVOKED".equalsIgnoreCase(license.getStatus())) {
            response.put("valid", false);
            response.put("message", "License has been revoked");
            return response;
        }

        LocalDateTime now = LocalDateTime.now();

        // Check if FREE license has already passed its fixed validUntil date
        if ("FREE".equalsIgnoreCase(license.getLicenseType()) && license.getValidUntil() != null && now.isAfter(license.getValidUntil())) {
            license.setStatus("EXPIRED");
            licenseRepository.save(license);
            response.put("valid", false);
            response.put("message", "Free license expired on " + license.getValidUntil());
            return response;
        }

        // 1. Pending Activation
        if ("PENDING".equalsIgnoreCase(license.getStatus())) {
            license.setDeviceId(deviceId);
            license.setActivationDate(now);

            if ("FREE".equalsIgnoreCase(license.getLicenseType())) {
                license.setExpiryDate(license.getValidUntil() != null ? license.getValidUntil() : now.plusDays(30));
            } else {
                int months = license.getSubscriptionDurationMonths() != null ? license.getSubscriptionDurationMonths() : 12;
                license.setExpiryDate(now.plusMonths(months));
            }

            license.setStatus("ACTIVE");
            licenseRepository.save(license);

            response.put("valid", true);
            response.put("message", "License successfully activated for device");
            response.put("activationCode", license.getActivationCode());
            response.put("licenseType", license.getLicenseType());
            response.put("expiryDate", license.getExpiryDate());
            response.put("durationMonths", license.getSubscriptionDurationMonths());
            return response;
        }

        // 2. Already Active
        if ("ACTIVE".equalsIgnoreCase(license.getStatus())) {
            if (license.getExpiryDate() != null && now.isAfter(license.getExpiryDate())) {
                license.setStatus("EXPIRED");
                licenseRepository.save(license);

                response.put("valid", false);
                response.put("message", "License has expired");
                return response;
            }

            if (license.getDeviceId() != null && !license.getDeviceId().equals(deviceId)) {
                response.put("valid", false);
                response.put("message", "License is bound to another device");
                return response;
            }

            response.put("valid", true);
            response.put("message", "License active");
            response.put("activationCode", license.getActivationCode());
            response.put("licenseType", license.getLicenseType());
            response.put("expiryDate", license.getExpiryDate());
            response.put("durationMonths", license.getSubscriptionDurationMonths());
            return response;
        }

        response.put("valid", false);
        response.put("message", "License status: " + license.getStatus());
        return response;
    }

    public List<Map<String, Object>> validateAndActivateMultiLicenses(List<String> activationCodes, String deviceId) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (activationCodes == null || activationCodes.isEmpty()) {
            return results;
        }
        for (String code : activationCodes) {
            if (code != null && !code.trim().isEmpty()) {
                results.add(validateAndActivateLicense(code.trim(), deviceId));
            }
        }
        return results;
    }
}
