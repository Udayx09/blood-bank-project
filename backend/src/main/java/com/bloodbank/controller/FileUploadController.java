package com.bloodbank.controller;

import com.bloodbank.entity.Donor;
import com.bloodbank.repository.DonorRepository;
import com.bloodbank.security.DonorPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * File Upload Controller for handling profile photo uploads
 */
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final DonorRepository donorRepository;
    private static final String UPLOAD_DIR = "uploads/profile-photos/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public FileUploadController(DonorRepository donorRepository) {
        this.donorRepository = donorRepository;
    }

    /**
     * Upload profile photo for authenticated donor
     */
    @PostMapping("/profile-photo")
    public ResponseEntity<Map<String, Object>> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal DonorPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not authenticated"));
        }

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No file provided"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "File too large. Max 5MB allowed"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Only image files are allowed"));
        }

        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = "donor_" + principal.getId() + "_" + UUID.randomUUID().toString().substring(0, 8)
                    + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update donor profile photo URL
            Donor donor = donorRepository.findById(principal.getId()).orElse(null);
            if (donor != null) {
                // Delete old photo if exists
                if (donor.getProfilePhoto() != null) {
                    try {
                        Path oldPath = Paths.get(donor.getProfilePhoto().replace("/api/upload/files/", UPLOAD_DIR));
                        Files.deleteIfExists(oldPath);
                    } catch (Exception ignored) {
                    }
                }

                donor.setProfilePhoto("/api/upload/files/" + filename);
                donor.setUpdatedAt(LocalDateTime.now());
                donorRepository.save(donor);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Photo uploaded successfully",
                    "photoUrl", "/api/upload/files/" + filename));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to upload file"));
        }
    }

    /**
     * Serve uploaded files
     */
    @GetMapping("/files/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .header("Content-Type", contentType != null ? contentType : "image/jpeg")
                    .header("Cache-Control", "max-age=86400")
                    .body(fileContent);

        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    // ==================== PRESCRIPTION UPLOAD ====================
    private static final String PRESCRIPTION_DIR = "uploads/prescriptions/";

    /**
     * Upload prescription file (image or PDF) - public endpoint
     */
    @PostMapping("/prescription")
    public ResponseEntity<Map<String, Object>> uploadPrescription(
            @RequestParam("file") MultipartFile file) {

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No file provided"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "File too large. Max 5MB allowed"));
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Only images or PDF files are allowed"));
        }

        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(PRESCRIPTION_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = "rx_" + UUID.randomUUID().toString().substring(0, 12) + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Prescription uploaded successfully",
                    "filename", filename,
                    "url", "/api/upload/prescription/" + filename));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed to upload file"));
        }
    }

    /**
     * Serve prescription files
     */
    @GetMapping("/prescription/{filename}")
    public ResponseEntity<byte[]> getPrescription(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(PRESCRIPTION_DIR).resolve(filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                    .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                    .body(fileContent);

        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
