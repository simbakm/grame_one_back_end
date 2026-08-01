package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.GradeVersion;
import com.example.grameone_backend.repository.GradeVersionRepository;
import com.example.grameone_backend.service.LicenseService;
import com.example.grameone_backend.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/distribution")
@RequiredArgsConstructor
public class DistributionController {

    private final GradeVersionRepository gradeVersionRepository;
    private final LicenseService licenseService;
    private final MediaService mediaService;

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
        // Return the backend proxy download URL, not the raw R2 URL
        response.put("downloadUrl", "/api/distribution/grades/" + gradeId + "/download");
        response.put("packageSizeBytes", latest.getPackageSizeBytes());
        response.put("checksumSha256", latest.getChecksumSha256());
        response.put("publishedAt", latest.getPublishedAt());
        response.put("changelog", latest.getChangelog());

        return ResponseEntity.ok(response);
    }

    /**
     * Streams the latest grade content ZIP directly from R2.
     * The mobile app downloads from this endpoint instead of hitting R2 directly.
     */
    @GetMapping("/grades/{gradeId}/download")
    public ResponseEntity<StreamingResponseBody> downloadGradePackage(@PathVariable Long gradeId) {

        Optional<GradeVersion> latestOpt = gradeVersionRepository.findByGradeIdAndIsLatestTrue(gradeId);
        if (latestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        GradeVersion latest = latestOpt.get();
        String r2Url = latest.getPackageR2Url();

        if (r2Url == null || r2Url.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        // Extract the R2 object key from the stored URL
        // packageR2Url is the full URL: endpoint/bucket/key
        // We need just the key part (after endpoint/bucket/)
        String r2Key = extractR2Key(r2Url);

        StreamingResponseBody stream = outputStream -> {
            try (InputStream in = mediaService.downloadFileStream(r2Key)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        };

        String filename = "grade" + gradeId + "_v" + latest.getVersion() + ".zip";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH,
                        latest.getPackageSizeBytes() != null ? String.valueOf(latest.getPackageSizeBytes()) : "")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    /**
     * Extracts the R2 object key from a full R2 URL.
     * URL format: https://endpoint/bucketName/path/to/object
     * Returns: path/to/object
     */
    private String extractR2Key(String r2Url) {
        // Strip protocol and host, then strip bucket name prefix
        try {
            java.net.URI uri = new java.net.URI(r2Url);
            String path = uri.getPath(); // e.g. /grameone/packages/grade7_v1.0.zip
            if (path.startsWith("/")) path = path.substring(1);
            int slash = path.indexOf('/');
            if (slash >= 0) return path.substring(slash + 1); // strip bucket name
            return path;
        } catch (Exception e) {
            // Fallback: just use the last path segment
            return r2Url.substring(r2Url.lastIndexOf('/') + 1);
        }
    }
}
