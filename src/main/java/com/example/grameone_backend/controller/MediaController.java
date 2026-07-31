package com.example.grameone_backend.controller;

import com.example.grameone_backend.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "images") String folder) {
        try {
            String key = mediaService.uploadMedia(file, folder);
            String url = mediaService.getPublicUrl(key);

            Map<String, String> response = new HashMap<>();
            response.put("key", key);
            response.put("url", url);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to upload media: " + e.getMessage());
        }
    }
}
