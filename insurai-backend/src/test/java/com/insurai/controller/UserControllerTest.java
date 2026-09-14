package com.insurai.controller;

import com.insurai.model.User;
import com.insurai.repository.UserRepository;
import com.insurai.security.JwtTokenProvider;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_userctrl;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=InsurAI_Super_Secret_Jwt_Key_2026_Must_Be_At_Least_32_Bytes_Long_Test!"
})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User userA;
    private User userB;
    private User superAdmin;

    private String tokenUserA;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        // Create User A
        userA = userRepository.findByEmail("usera@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("User A");
            u.setEmail("usera@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("USER");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        // Create User B
        userB = userRepository.findByEmail("userb@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("User B");
            u.setEmail("userb@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("USER");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        // Create Super Admin
        superAdmin = userRepository.findByEmail("adminuser@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin User");
            u.setEmail("adminuser@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("SUPER_ADMIN");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        tokenUserA = jwtTokenProvider.generateToken(userA.getEmail(), userA.getRole(), userA.getId());
        tokenAdmin = jwtTokenProvider.generateToken(superAdmin.getEmail(), superAdmin.getRole(), superAdmin.getId());
    }

    @Test
    void testUserEditingOwnProfileReturns200() throws Exception {
        String jsonBody = "{\"name\":\"User A Updated\",\"phone\":\"9998887776\"}";

        mockMvc.perform(put("/api/users/" + userA.getId())
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User A Updated"))
                .andExpect(jsonPath("$.phone").value("9998887776"));
    }

    @Test
    void testUserEditingOtherUserProfileReturns403() throws Exception {
        String jsonBody = "{\"name\":\"Hacked Name\"}";

        mockMvc.perform(put("/api/users/" + userB.getId())
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminEditingOtherUserProfileReturns200() throws Exception {
        String jsonBody = "{\"name\":\"User B Fixed By Admin\"}";

        mockMvc.perform(put("/api/users/" + userB.getId())
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User B Fixed By Admin"));
    }
}
