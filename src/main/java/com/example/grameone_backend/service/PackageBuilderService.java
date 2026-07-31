package com.example.grameone_backend.service;

import com.example.grameone_backend.entity.Grade;
import com.example.grameone_backend.entity.GradeVersion;
import com.example.grameone_backend.repository.GradeRepository;
import com.example.grameone_backend.repository.GradeVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class PackageBuilderService {

    private final GradeRepository gradeRepository;
    private final GradeVersionRepository gradeVersionRepository;
    private final SqliteGeneratorService sqliteGeneratorService;
    private final MediaService mediaService;
    private final ObjectMapper objectMapper;

    @Transactional
    public GradeVersion publishGradePackage(Long gradeId, String changelog) throws Exception {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Grade not found with ID: " + gradeId));

        // Determine next version number (e.g. 1.0 -> 1.1)
        Optional<GradeVersion> latestOpt = gradeVersionRepository.findByGradeIdAndIsLatestTrue(gradeId);
        String nextVersion = "1.0";
        if (latestOpt.isPresent()) {
            String currentVersion = latestOpt.get().getVersion();
            try {
                double v = Double.parseDouble(currentVersion);
                nextVersion = String.format(Locale.US, "%.1f", v + 0.1);
            } catch (NumberFormatException e) {
                nextVersion = currentVersion + ".1";
            }
        }

        // Create temp workspace directory
        Path tempDir = Files.createTempDirectory("grameone_package_");
        File databaseDir = new File(tempDir.toFile(), "database");
        File assetsImagesDir = new File(tempDir.toFile(), "assets/images");
        File assetsDiagramsDir = new File(tempDir.toFile(), "assets/diagrams");

        databaseDir.mkdirs();
        assetsImagesDir.mkdirs();
        assetsDiagramsDir.mkdirs();

        // 1. Generate SQLite database content.db
        File contentDbFile = new File(databaseDir, "content.db");
        sqliteGeneratorService.generateSqliteDatabaseForGrade(gradeId, contentDbFile);

        // 2. Generate metadata.json
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("gradeId", grade.getId());
        metadata.put("gradeName", grade.getName());
        metadata.put("version", nextVersion);
        metadata.put("publishedAt", LocalDateTime.now().toString());
        metadata.put("changelog", changelog != null ? changelog : "Initial release");

        File metadataFile = new File(tempDir.toFile(), "metadata.json");
        objectMapper.writeValue(metadataFile, metadata);

        // 3. Compress package into ZIP archive
        File zipOutputFile = File.createTempFile("grade" + gradeId + "_v" + nextVersion + "_", ".zip");
        createZipArchive(tempDir.toFile(), zipOutputFile);

        // 4. Calculate SHA256 checksum & file size
        String sha256Checksum = calculateSha256(zipOutputFile);
        long zipSizeBytes = zipOutputFile.length();

        // 5. Upload ZIP package to Cloudflare R2
        String r2Key = "packages/grade" + gradeId + "_v" + nextVersion + ".zip";
        byte[] zipBytes = Files.readAllBytes(zipOutputFile.toPath());
        mediaService.uploadFile(zipBytes, r2Key, "application/zip");
        String packagePublicUrl = mediaService.getPublicUrl(r2Key);

        // 6. Update previous versions and save new version record
        if (latestOpt.isPresent()) {
            GradeVersion prev = latestOpt.get();
            prev.setIsLatest(false);
            gradeVersionRepository.save(prev);
        }

        GradeVersion newVersionRecord = GradeVersion.builder()
                .grade(grade)
                .gradeName(grade.getName())
                .version(nextVersion)
                .packageR2Url(packagePublicUrl)
                .packageSizeBytes(zipSizeBytes)
                .checksumSha256(sha256Checksum)
                .changelog(changelog != null ? changelog : "Published version " + nextVersion)
                .isLatest(true)
                .publishedAt(LocalDateTime.now())
                .build();

        GradeVersion savedVersion = gradeVersionRepository.save(newVersionRecord);

        // Clean up temporary files
        deleteDirectoryRecursively(tempDir.toFile());
        zipOutputFile.delete();

        return savedVersion;
    }

    public List<GradeVersion> getVersionHistory(Long gradeId) {
        return gradeVersionRepository.findByGradeIdOrderByPublishedAtDesc(gradeId);
    }

    @Transactional
    public GradeVersion rollbackVersion(Long gradeId, String version) {
        GradeVersion targetVersion = gradeVersionRepository.findByGradeIdAndVersion(gradeId, version)
                .orElseThrow(() -> new RuntimeException("Version not found: " + version));

        Optional<GradeVersion> currentLatest = gradeVersionRepository.findByGradeIdAndIsLatestTrue(gradeId);
        if (currentLatest.isPresent()) {
            GradeVersion curr = currentLatest.get();
            curr.setIsLatest(false);
            gradeVersionRepository.save(curr);
        }

        targetVersion.setIsLatest(true);
        return gradeVersionRepository.save(targetVersion);
    }

    private void createZipArchive(File sourceDir, File zipOutputFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipOutputFile))) {
            Path sourcePath = sourceDir.toPath();
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString().replace("\\", "/"));
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private String calculateSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = new FileInputStream(file);
             DigestInputStream dis = new DigestInputStream(is, digest)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {}
        }
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void deleteDirectoryRecursively(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDirectoryRecursively(f);
            }
        }
        file.delete();
    }
}
