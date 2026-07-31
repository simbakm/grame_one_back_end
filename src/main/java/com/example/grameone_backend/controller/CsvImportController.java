package com.example.grameone_backend.controller;

import com.example.grameone_backend.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/csv")
@RequiredArgsConstructor
public class CsvImportController {

    private final CsvImportService csvImportService;

    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("CSV file is empty");
        }
        try {
            Map<String, Object> result = csvImportService.importQuestionsFromCsv(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to import CSV: " + e.getMessage());
        }
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        String csvContent = "Grade,Subject,Topic,Unit,Concept,QuestionNumber,QuestionText,OptionA,OptionB,OptionC,OptionD,CorrectOption,Difficulty,Explanation\n" +
                "Grade 7,Mathematics,Numbers & Algebra,Unit 1: Addition,Solving Linear Equations,1,What is 2x + 5 when x = 3?,10,11,12,13,B,MEDIUM,Substitute x = 3 into 2(3) + 5 = 11.\n" +
                "Grade 7,Mathematics,Numbers & Algebra,Unit 1: Addition,Solving Linear Equations,2,Solve for y: y / 4 = 5,15,20,25,10,B,EASY,Multiply both sides by 4 to get y = 20.\n";

        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grameone_questions_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
