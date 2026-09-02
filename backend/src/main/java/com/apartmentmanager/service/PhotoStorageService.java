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

    public String storeImage(MultipartFile file) throws IOException {
        byte[] header = file.getInputStream().readNBytes(16);
        if (!isValidImage(header)) {
            throw new IOException("Uploaded file is not a supported image format");
        }
        return store(file);
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
        if (n.endsWith(".heic")) return org.springframework.http.MediaType.parseMediaType("image/heic");
        if (n.endsWith(".heif")) return org.springframework.http.MediaType.parseMediaType("image/heif");
        return org.springframework.http.MediaType.IMAGE_JPEG;
    }

    private static boolean isValidImage(byte[] header) {
        if (header.length < 4) return false;
        // JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) return true;
        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return true;
        // GIF: "GIF"
        if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) return true;
        // BMP: "BM"
        if (header[0] == 0x42 && header[1] == 0x4D) return true;
        // WebP: "RIFF" at 0, "WEBP" at 8
        if (header.length >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) return true;
        // AVIF/HEIC/HEIF: ISO BMFF "ftyp" box at offset 4
        if (header.length >= 8 && header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) return true;
        return false;
    }

    public void delete(String fileName) {
        try {
            Files.deleteIfExists(load(fileName));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + fileName, e);
        }
    }
}
