package com.insurai.controller;

import com.insurai.model.Company;
import com.insurai.model.User;
import com.insurai.repository.CompanyRepository;
import com.insurai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=InsurAI_Super_Secret_Jwt_Key_2026_Must_Be_At_Least_32_Bytes_Long_Test!"
})
public class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByEmail("seededuser@example.com").isEmpty()) {
            User user = new User();
            user.setName("Test User");
            user.setEmail("seededuser@example.com");
            user.setPassword(passwordEncoder.encode("SecretPass123"));
            user.setRole("USER");
            user.setIsActive(true);
            userRepository.save(user);
        }

        if (companyRepository.findByEmail("seededcompany@example.com").isEmpty()) {
            Company company = new Company();
            company.setName("Test Insurance Co");
            company.setEmail("seededcompany@example.com");
            company.setPassword(passwordEncoder.encode("CompanyPass123"));
            company.setStatus("APPROVED");
            company.setIsActive(true);
            companyRepository.save(company);
        }
    }

    @Test
    void testValidUserLoginReturns200AndToken() throws Exception {
        String jsonPayload = "{\"email\":\"seededuser@example.com\",\"password\":\"SecretPass123\"}";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("seededuser@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void testInvalidUserPasswordReturns401() throws Exception {
        String jsonPayload = "{\"email\":\"seededuser@example.com\",\"password\":\"WrongPassword\"}";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testValidCompanyLoginReturns200AndToken() throws Exception {
        String jsonPayload = "{\"email\":\"seededcompany@example.com\",\"password\":\"CompanyPass123\"}";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("seededcompany@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }
}
