package com.apartmentmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class PhotoStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID() + ext;
        Path targetPath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath);

        return fileName;
    }

    public Path load(String fileName) {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName);
    }

    public org.springframework.http.MediaType contentType(String fileName) {
        String n = fileName == null ? "" : fileName.toLowerCase();
        if (n.endsWith(".png")) return org.springframework.http.MediaType.IMAGE_PNG;
        if (n.endsWith(".gif")) return org.springframework.http.MediaType.IMAGE_GIF;
        if (n.endsWith(".webp")) return org.springframework.http.MediaType.parseMediaType("image/webp");
        if (n.endsWith(".bmp")) return org.springframework.http.MediaType.parseMediaType("image/bmp");
        if (n.endsWith(".avif")) return org.springframework.http.MediaType.parseMediaType("image/avif");
        if (n.endsWith(".heic") || n.endsWith(".heif")) return org.springframework.http.MediaType.parseMediaType("image/heic");
        return org.springframework.http.MediaType.IMAGE_JPEG;
    }

    public void delete(String fileName) {
        try {
            Files.deleteIfExists(load(fileName));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + fileName, e);
        }
    }
}
