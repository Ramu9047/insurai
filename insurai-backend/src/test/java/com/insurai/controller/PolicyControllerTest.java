package com.insurai.controller;

import com.insurai.model.Policy;
import com.insurai.model.User;
import com.insurai.model.UserPolicy;
import com.insurai.repository.PolicyRepository;
import com.insurai.repository.UserPolicyRepository;
import com.insurai.repository.UserRepository;
import com.insurai.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_policyctrl;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=InsurAI_Super_Secret_Jwt_Key_2026_Must_Be_At_Least_32_Bytes_Long_Test!",
    "app.upload.base-url=http://localhost:8080/uploads/"
})
public class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private UserPolicyRepository userPolicyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User userA;
    private User userB;
    private Policy testPolicy;
    private UserPolicy userAPolicy;
    private UserPolicy userBPolicy;

    private String tokenUserA;
    private String tokenUserB;

    @BeforeEach
    void setUp() {
        userA = userRepository.findByEmail("policy_usera@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Policy User A");
            u.setEmail("policy_usera@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("USER");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        userB = userRepository.findByEmail("policy_userb@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Policy User B");
            u.setEmail("policy_userb@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("USER");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        tokenUserA = jwtTokenProvider.generateToken(userA.getEmail(), userA.getRole(), userA.getId());
        tokenUserB = jwtTokenProvider.generateToken(userB.getEmail(), userB.getRole(), userB.getId());

        testPolicy = new Policy();
        testPolicy.setName("Health Shield Pro");
        testPolicy.setCategory("Health");
        testPolicy.setType("Comprehensive");
        testPolicy.setPremium(500.0);
        testPolicy.setCoverage(100000.0);
        testPolicy.setStatus("ACTIVE");
        testPolicy = policyRepository.save(testPolicy);

        userAPolicy = new UserPolicy();
        userAPolicy.setUser(userA);
        userAPolicy.setPolicy(testPolicy);
        userAPolicy.setStartDate(LocalDate.now());
        userAPolicy.setPurchasedAt(java.time.LocalDateTime.now());
        userAPolicy.setStatus("PENDING_PAYMENT");
        userAPolicy = userPolicyRepository.save(userAPolicy);

        userBPolicy = new UserPolicy();
        userBPolicy.setUser(userB);
        userBPolicy.setPolicy(testPolicy);
        userBPolicy.setStartDate(LocalDate.now());
        userBPolicy.setPurchasedAt(java.time.LocalDateTime.now());
        userBPolicy.setStatus("PENDING_PAYMENT");
        userBPolicy = userPolicyRepository.save(userBPolicy);
    }

    // --- 1. buyPolicy ---
    @Test
    void testBuyPolicyWrongUserReturns403() throws Exception {
        mockMvc.perform(post("/api/policies/" + testPolicy.getId() + "/buy/" + userB.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());
    }

    @Test
    void testBuyPolicyRightUserReturns200() throws Exception {
        mockMvc.perform(post("/api/policies/" + testPolicy.getId() + "/buy/" + userA.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk());
    }

    // --- 2. quotePolicy ---
    @Test
    void testQuotePolicyWrongUserReturns403() throws Exception {
        mockMvc.perform(post("/api/policies/" + testPolicy.getId() + "/quote/" + userB.getId())
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Need quote for User B\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testQuotePolicyRightUserReturns200() throws Exception {
        mockMvc.perform(post("/api/policies/" + testPolicy.getId() + "/quote/" + userA.getId())
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Need quote for myself\"}"))
                .andExpect(status().isOk());
    }

    // --- 3. purchasePolicy ---
    @Test
    void testPurchasePolicyWrongUserReturns403() throws Exception {
        mockMvc.perform(post("/api/policies/" + userBPolicy.getId() + "/purchase")
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPurchasePolicyRightUserReturns200() throws Exception {
        mockMvc.perform(post("/api/policies/" + userAPolicy.getId() + "/purchase")
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk());
    }

    // --- 4. getUserPolicies ---
    @Test
    void testGetUserPoliciesWrongUserReturns403() throws Exception {
        mockMvc.perform(get("/api/policies/user/" + userB.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserPoliciesRightUserReturns200() throws Exception {
        mockMvc.perform(get("/api/policies/user/" + userA.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk());
    }

    // --- 5. getRecommendations ---
    @Test
    void testGetRecommendationsWrongUserReturns403() throws Exception {
        mockMvc.perform(get("/api/policies/recommendations/" + userB.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetRecommendationsRightUserReturns200() throws Exception {
        mockMvc.perform(get("/api/policies/recommendations/" + userA.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk());
    }

    // --- 6. filterPolicies ---
    @Test
    void testFilterPoliciesWrongUserReturns403() throws Exception {
        mockMvc.perform(post("/api/policies/filter/" + userB.getId())
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testFilterPoliciesRightUserReturns200() throws Exception {
        mockMvc.perform(post("/api/policies/filter/" + userA.getId())
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    // --- 7. uploadDocument ---
    @Test
    void testUploadDocumentWrongUserReturns403() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "dummy pdf content".getBytes());

        mockMvc.perform(multipart("/api/policies/upload/" + userBPolicy.getId())
                .file(mockFile)
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUploadDocumentRightUserReturns200() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "dummy pdf content".getBytes());

        mockMvc.perform(multipart("/api/policies/upload/" + userAPolicy.getId())
                .file(mockFile)
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk());
    }
}
