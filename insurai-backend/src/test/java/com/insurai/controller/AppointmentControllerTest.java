package com.insurai.controller;

import com.insurai.model.Booking;
import com.insurai.model.User;
import com.insurai.repository.BookingRepository;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_apptctrl;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=InsurAI_Super_Secret_Jwt_Key_2026_Must_Be_At_Least_32_Bytes_Long_Test!"
})
public class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User clientUser;
    private User otherUser;
    private User assignedAgent;
    private User wrongAgent;

    private Booking booking;

    private String tokenClientUser;
    private String tokenOtherUser;
    private String tokenAssignedAgent;
    private String tokenWrongAgent;

    @BeforeEach
    void setUp() {
        clientUser = userRepository.findByEmail("appt_client@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Appt Client");
            u.setEmail("appt_client@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("USER");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        otherUser = userRepository.findByEmail("appt_otheruser@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Appt Other User");
            u.setEmail("appt_otheruser@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("USER");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        assignedAgent = userRepository.findByEmail("appt_agent1@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Assigned Agent");
            u.setEmail("appt_agent1@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("AGENT");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        wrongAgent = userRepository.findByEmail("appt_agent2@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Wrong Agent");
            u.setEmail("appt_agent2@example.com");
            u.setPassword(passwordEncoder.encode("Password123"));
            u.setRole("AGENT");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        tokenClientUser = jwtTokenProvider.generateToken(clientUser.getEmail(), clientUser.getRole(), clientUser.getId());
        tokenOtherUser = jwtTokenProvider.generateToken(otherUser.getEmail(), otherUser.getRole(), otherUser.getId());
        tokenAssignedAgent = jwtTokenProvider.generateToken(assignedAgent.getEmail(), assignedAgent.getRole(), assignedAgent.getId());
        tokenWrongAgent = jwtTokenProvider.generateToken(wrongAgent.getEmail(), wrongAgent.getRole(), wrongAgent.getId());

        booking = new Booking();
        booking.setUser(clientUser);
        booking.setAgent(assignedAgent);
        booking.setStartTime(LocalDateTime.now().plusDays(5));
        booking.setEndTime(LocalDateTime.now().plusDays(5).plusHours(1));
        booking.setStatus("REQUESTED");
        booking.setReason("Consultation");
        booking = bookingRepository.save(booking);
    }

    @Test
    void testWrongAgentApproveAttemptReturns403() throws Exception {
        String body = "{\"notes\":\"Approved by wrong agent\"}";

        mockMvc.perform(put("/api/appointments/" + booking.getId() + "/approve")
                .header("Authorization", "Bearer " + tokenWrongAgent)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAssignedAgentApproveAttemptReturns200() throws Exception {
        String body = "{\"notes\":\"Approved by assigned agent\"}";

        mockMvc.perform(put("/api/appointments/" + booking.getId() + "/approve")
                .header("Authorization", "Bearer " + tokenAssignedAgent)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void testWrongAgentRejectAttemptReturns403() throws Exception {
        String body = "{\"rejectionReason\":\"Not qualified\",\"includeAIRecommendations\":false}";

        mockMvc.perform(put("/api/appointments/" + booking.getId() + "/reject")
                .header("Authorization", "Bearer " + tokenWrongAgent)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAssignedAgentRejectAttemptReturns200() throws Exception {
        String body = "{\"rejectionReason\":\"Not qualified\",\"includeAIRecommendations\":false}";

        mockMvc.perform(put("/api/appointments/" + booking.getId() + "/reject")
                .header("Authorization", "Bearer " + tokenAssignedAgent)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void testWrongAgentCompleteAttemptReturns403() throws Exception {
        String body = "{\"consultationNotes\":\"Completed by wrong agent\"}";

        mockMvc.perform(put("/api/appointments/" + booking.getId() + "/complete")
                .header("Authorization", "Bearer " + tokenWrongAgent)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAssignedAgentCompleteAttemptReturns200() throws Exception {
        String body = "{\"consultationNotes\":\"Completed by assigned agent\"}";

        mockMvc.perform(put("/api/appointments/" + booking.getId() + "/complete")
                .header("Authorization", "Bearer " + tokenAssignedAgent)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void testNonParticipantRescheduleAttemptReturns403() throws Exception {
        String newStart = LocalDateTime.now().plusDays(6).toString();
        String newEnd = LocalDateTime.now().plusDays(6).plusHours(1).toString();
        String body = String.format("{\"start\":\"%s\",\"end\":\"%s\"}", newStart, newEnd);

        mockMvc.perform(put("/api/bookings/" + booking.getId() + "/reschedule")
                .header("Authorization", "Bearer " + tokenOtherUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void testParticipantRescheduleAttemptReturns200() throws Exception {
        String newStart = LocalDateTime.now().plusDays(6).toString();
        String newEnd = LocalDateTime.now().plusDays(6).plusHours(1).toString();
        String body = String.format("{\"start\":\"%s\",\"end\":\"%s\"}", newStart, newEnd);

        mockMvc.perform(put("/api/bookings/" + booking.getId() + "/reschedule")
                .header("Authorization", "Bearer " + tokenClientUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }
}
