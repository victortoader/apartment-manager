package com.apartmentmanager;

import com.apartmentmanager.service.PhotoStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhotoStorageServiceTest {

    private static final byte[] PNG_BYTES = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13
    };
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};

    @TempDir
    Path tempDir;

    private PhotoStorageService service() {
        PhotoStorageService s = new PhotoStorageService();
        try {
            java.lang.reflect.Field f = PhotoStorageService.class.getDeclaredField("uploadDir");
            f.setAccessible(true);
            f.set(s, tempDir.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    @Test
    void jpgReturnsImageJpeg() {
        assertEquals(MediaType.IMAGE_JPEG, service().contentType("photo.jpg"));
    }

    @Test
    void jpegReturnsImageJpeg() {
        assertEquals(MediaType.IMAGE_JPEG, service().contentType("photo.jpeg"));
    }

    @Test
    void pngReturnsImagePng() {
        assertEquals(MediaType.IMAGE_PNG, service().contentType("photo.png"));
    }

    @Test
    void gifReturnsImageGif() {
        assertEquals(MediaType.IMAGE_GIF, service().contentType("photo.gif"));
    }

    @Test
    void webpReturnsImageWebp() {
        assertEquals(MediaType.parseMediaType("image/webp"), service().contentType("photo.webp"));
    }

    @Test
    void bmpReturnsImageBmp() {
        assertEquals(MediaType.parseMediaType("image/bmp"), service().contentType("photo.bmp"));
    }

    @Test
    void avifReturnsImageAvif() {
        assertEquals(MediaType.parseMediaType("image/avif"), service().contentType("photo.avif"));
    }

    @Test
    void heicReturnsImageHeic() {
        assertEquals(MediaType.parseMediaType("image/heic"), service().contentType("photo.heic"));
    }

    @Test
    void heifReturnsImageHeif() {
        assertEquals(MediaType.parseMediaType("image/heif"), service().contentType("photo.heif"));
    }

    @Test
    void unknownExtensionFallsBackToJpeg() {
        assertEquals(MediaType.IMAGE_JPEG, service().contentType("photo.xyz"));
    }

    @Test
    void nullFileNameFallsBackToJpeg() {
        assertEquals(MediaType.IMAGE_JPEG, service().contentType(null));
    }

    @Test
    void storeImage_validJpeg_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);
        String stored = service().storeImage(file);
        assertEquals(".jpg", stored.substring(stored.lastIndexOf('.')), "Stored name keeps the original extension");
    }

    @Test
    void storeImage_nonImage_throws() {
        PhotoStorageService s = service();
        MockMultipartFile file = new MockMultipartFile("file", "evil.txt", "text/plain", "not an image".getBytes());
        assertThrows(java.io.IOException.class, () -> s.storeImage(file), "Non-image upload must be rejected");
    }

    @Test
    void store_document_pdfStillAccepted() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "handover.pdf", "application/pdf", "%PDF-1.4 fake".getBytes());
        String stored = service().store(pdf);
        assertEquals(".pdf", stored.substring(stored.lastIndexOf('.')), "Generic store must accept documents such as PDFs");
    }
}