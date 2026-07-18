package com.example.demo;

import com.example.demo.model.Apartment;
import com.example.demo.model.User;
import com.example.demo.repository.ApartmentRepository;
import com.example.demo.repository.HandoverProtocolRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import jakarta.persistence.EntityManager;
import java.nio.file.Path;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
abstract class AbstractIntegrationTest {

    @Autowired protected WebApplicationContext context;
    @Autowired protected JwtUtil jwtUtil;
    @Autowired protected UserRepository userRepository;
    @Autowired protected ApartmentRepository apartmentRepository;
    @Autowired protected HandoverProtocolRepository protocolRepository;
    @Autowired protected EntityManager entityManager;
    @Autowired protected ObjectMapper objectMapper;

    protected MockMvc mockMvc;

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", () -> tempDir.toString());
    }

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    protected String bearer(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return "Bearer " + jwtUtil.generateToken(username, user.getRole());
    }

    protected Apartment createApartment(String title) {
        Apartment apt = new Apartment(title, "Desc", "Location", 1000.0, 2, 50.0);
        return apartmentRepository.save(apt);
    }
}
