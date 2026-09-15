package com.insurai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Shared File Storage Service
 * Centralizes file upload sanitization, path-traversal prevention,
 * extension validation, and URL construction across all controllers.
 */
@Service
public class FileStorageService {

    private final String uploadBaseUrl;
    private final Path uploadsDir;

    public FileStorageService(@Value("${app.upload.base-url:http://localhost:8080/uploads/}") String uploadBaseUrl) {
        this.uploadBaseUrl = uploadBaseUrl.endsWith("/") ? uploadBaseUrl : uploadBaseUrl + "/";
        this.uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
    }

    /**
     * Store file securely with path traversal protection and extension whitelisting
     *
     * @param file Incoming MultipartFile
     * @return Full access URL for stored file
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }

        String rawName = file.getOriginalFilename();
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        // Sanitize filename to prevent path traversal
        String safeName = Paths.get(rawName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String ext = "";
        int dotIdx = safeName.lastIndexOf('.');
        if (dotIdx > 0) {
            ext = safeName.substring(dotIdx + 1).toLowerCase();
        }

        List<String> allowedExtensions = List.of("pdf", "jpg", "jpeg", "png", "doc", "docx");
        if (!allowedExtensions.contains(ext)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported file format. Allowed: pdf, jpg, jpeg, png, doc, docx");
        }

        String fileName = System.currentTimeMillis() + "_" + safeName;
        Path targetPath = uploadsDir.resolve(fileName).normalize();

        if (!targetPath.startsWith(uploadsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload path");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }

        return uploadBaseUrl + fileName;
    }
}
