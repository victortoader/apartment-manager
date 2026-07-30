package com.apartmentmanager;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.ApartmentRepository;
import com.apartmentmanager.repository.HandoverProtocolRepository;
import com.apartmentmanager.repository.UserRepository;
import com.apartmentmanager.security.JwtUtil;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
abstract class AbstractIntegrationTest {

    protected static final String TEST_PASSWORD;

    static {
        String password = System.getenv("DEFAULT_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("DEFAULT_PASSWORD environment variable is required to run tests");
        }
        TEST_PASSWORD = password;
    }

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

    protected static String jsonResource(String name) throws IOException {
        try (var in = AbstractIntegrationTest.class.getResourceAsStream("/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected Apartment createApartment(String title) {
        Apartment apt = new Apartment(title, "Desc", "Location", 1000.0, 2, 50.0);
        return apartmentRepository.save(apt);
    }
}
