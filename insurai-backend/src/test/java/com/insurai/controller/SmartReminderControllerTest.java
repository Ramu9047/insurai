package com.insurai.controller;

import com.insurai.model.SmartReminder;
import com.insurai.model.User;
import com.insurai.repository.SmartReminderRepository;
import com.insurai.repository.UserRepository;
import com.insurai.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_remctrl;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=InsurAI_Super_Secret_Jwt_Key_2026_Must_Be_At_Least_32_Bytes_Long_Test!"
})
public class SmartReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmartReminderRepository smartReminderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User userA;
    private User userB;

    private SmartReminder reminderA;
    private SmartReminder reminderB;

    private String tokenUserA;

    @BeforeEach
    void setUp() {
        userA = userRepository.findByEmail("rem_usera@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("User A");
            u.setEmail("rem_usera@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("USER");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        userB = userRepository.findByEmail("rem_userb@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("User B");
            u.setEmail("rem_userb@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("USER");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        tokenUserA = jwtTokenProvider.generateToken(userA.getEmail(), userA.getRole(), userA.getId());

        reminderA = new SmartReminder();
        reminderA.setUser(userA);
        reminderA.setTitle("User A Reminder");
        reminderA.setMessage("Test message A");
        reminderA.setPriority("MEDIUM");
        reminderA.setType("APPOINTMENT");
        reminderA.setReminderTime(LocalDateTime.now().plusDays(1));
        reminderA.setSent(false);
        reminderA = smartReminderRepository.save(reminderA);

        reminderB = new SmartReminder();
        reminderB.setUser(userB);
        reminderB.setTitle("User B Reminder");
        reminderB.setMessage("Test message B");
        reminderB.setPriority("MEDIUM");
        reminderB.setType("APPOINTMENT");
        reminderB.setReminderTime(LocalDateTime.now().plusDays(1));
        reminderB.setSent(false);
        reminderB = smartReminderRepository.save(reminderB);
    }

    @Test
    void testUserMarkingOwnReminderAsSentReturns200() throws Exception {
        mockMvc.perform(put("/api/reminders/" + reminderA.getId() + "/mark-sent")
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk());
    }

    @Test
    void testUserMarkingOtherUserReminderAsSentReturns403() throws Exception {
        mockMvc.perform(put("/api/reminders/" + reminderB.getId() + "/mark-sent")
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserDeletingOwnReminderReturns200() throws Exception {
        mockMvc.perform(delete("/api/reminders/" + reminderA.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk());
    }

    @Test
    void testUserDeletingOtherUserReminderReturns403() throws Exception {
        mockMvc.perform(delete("/api/reminders/" + reminderB.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserFetchingPendingRemindersReturnsOnlyOwnEvenIfOtherUserIdPassed() throws Exception {
        mockMvc.perform(get("/api/reminders/pending")
                .param("userId", reminderB.getUser().getId().toString())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reminderA.getId()));
    }

    @Test
    void testUserFetchingAllRemindersReturnsOnlyOwnEvenIfOtherUserIdPassed() throws Exception {
        mockMvc.perform(get("/api/reminders/all")
                .param("userId", reminderB.getUser().getId().toString())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reminderA.getId()));
    }
}
