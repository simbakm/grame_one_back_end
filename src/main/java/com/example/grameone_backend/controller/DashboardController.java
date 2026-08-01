package com.example.grameone_backend.controller;

import com.example.grameone_backend.entity.Grade;
import com.example.grameone_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final QuestionRepository questionRepository;
    private final LicenseRepository licenseRepository;
    private final GradeRepository gradeRepository;
    private final GradeVersionRepository gradeVersionRepository;
    private final SubjectRepository subjectRepository;

    /**
     * Returns consolidated dashboard statistics including:
     * - totalQuestions, totalLicenses, activeLicenses, pendingLicenses, totalR2Packages
     * - activationsByMonth (last 6 months)
     * - contentDistribution (questions per grade)
     */
    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // ---- Core Counts ----
        long totalQuestions = questionRepository.count();
        long totalLicenses = licenseRepository.count();
        long activeLicenses = licenseRepository.countByStatus("ACTIVE");
        long pendingLicenses = licenseRepository.countByStatus("PENDING");
        long expiredLicenses = licenseRepository.countByStatus("EXPIRED");
        long totalR2Packages = gradeVersionRepository.countByIsLatestTrue();

        stats.put("totalQuestions", totalQuestions);
        stats.put("totalLicenses", totalLicenses);
        stats.put("activeLicenses", activeLicenses);
        stats.put("pendingLicenses", pendingLicenses);
        stats.put("expiredLicenses", expiredLicenses);
        stats.put("totalR2Packages", totalR2Packages);
        stats.put("totalGrades", gradeRepository.count());

        // ---- Activations By Month (last 6 months) ----
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Object[]> activationRows = licenseRepository.countActivationsByMonth(sixMonthsAgo);

        List<String> activationLabels = new ArrayList<>();
        List<Long> activationCounts = new ArrayList<>();
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (Object[] row : activationRows) {
            activationLabels.add(row[0] + "/" + row[1]); // "YYYY/MM"
            activationCounts.add(((Number) row[2]).longValue());
        }

        // If no data, build empty 6-month skeleton
        if (activationLabels.isEmpty()) {
            for (int i = 5; i >= 0; i--) {
                LocalDateTime month = LocalDateTime.now().minusMonths(i);
                activationLabels.add(month.format(labelFmt));
                activationCounts.add(0L);
            }
        } else {
            // Humanize labels: "2026/07" -> "Jul 2026"
            List<String> humanLabels = new ArrayList<>();
            for (String raw : activationLabels) {
                String[] parts = raw.split("/");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                humanLabels.add(LocalDateTime.of(year, month, 1, 0, 0).format(labelFmt));
            }
            activationLabels.clear();
            activationLabels.addAll(humanLabels);
        }

        Map<String, Object> activationsChart = new LinkedHashMap<>();
        activationsChart.put("labels", activationLabels);
        activationsChart.put("data", activationCounts);
        stats.put("activationsByMonth", activationsChart);

        // ---- Content Distribution By Grade (questions per grade) ----
        List<Grade> grades = gradeRepository.findAll();
        List<String> gradeLabels = new ArrayList<>();
        List<Long> gradeCounts = new ArrayList<>();

        for (Grade grade : grades) {
            long count = questionRepository.countByGradeId(grade.getId());
            gradeLabels.add(grade.getName());
            gradeCounts.add(count);
        }

        Map<String, Object> distributionChart = new LinkedHashMap<>();
        distributionChart.put("labels", gradeLabels);
        distributionChart.put("data", gradeCounts);
        stats.put("contentDistribution", distributionChart);

        // ---- Recent Activations (last 5 activated licenses) ----
        stats.put("recentActivations", licenseRepository.findTop5ByStatusOrderByActivationDateDesc("ACTIVE"));

        return stats;
    }
}
