package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PhotoTest extends AbstractIntegrationTest {

    private Apartment apt;

    @BeforeEach
    void setUp() {
        apt = createApartment("Photo Apt");
    }

    @Test
    void uploadPhotoAsOwner_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake image content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/photos").file(file)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoPaths", hasSize(1)));
    }

    @Test
    void uploadPhotoAsAdmin_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "fake image content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/photos").file(file)
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void uploadPhotoAsTenantOwnApartment_succeeds() throws Exception {
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/photos").file(file)
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isOk());
    }

    @Test
    void uploadPhotoAsTenantOtherApartment_returns403() throws Exception {
        User tenant = userRepository.findByUsername("tenant").orElseThrow();
        tenant.setApartment(apt);
        userRepository.save(tenant);

        Apartment other = createApartment("Other Apt");
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "content".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + other.getId() + "/photos").file(file)
                        .header("Authorization", bearer("tenant")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPhoto_isPublicEndpoint() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "image bytes here".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/photos").file(file)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk());

        Apartment updated = apartmentRepository.findById(apt.getId()).orElseThrow();
        String storedName = updated.getPhotoPaths().get(0);

        mockMvc.perform(get("/api/apartments/photos/" + storedName))
                .andExpect(status().isOk());
    }

    @Test
    void getPhoto_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/apartments/photos/nonexistent.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadMultiplePhotos_allSaved() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "photo1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "photo2.png", "image/png", "content2".getBytes());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/photos").file(file1)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/api/apartments/" + apt.getId() + "/photos").file(file2)
                        .header("Authorization", bearer("owner")))
                .andExpect(status().isOk());

        Apartment updated = apartmentRepository.findById(apt.getId()).orElseThrow();
        assert updated.getPhotoPaths().size() == 2;
    }
}
