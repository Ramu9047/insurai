package com.insurai.controller;

import com.insurai.model.Booking;
import com.insurai.repository.BookingRepository;
import com.insurai.service.GoogleCalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST Controller for Meeting Management
 * Handles meeting creation and retrieval
 */
@RestController
@RequestMapping("/api/meeting")
public class MeetingController {

    private final GoogleCalendarService calendarService;
    private final BookingRepository bookingRepository;

    public MeetingController(GoogleCalendarService calendarService, BookingRepository bookingRepository) {
        this.calendarService = calendarService;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Create a meeting for an appointment
     * POST /api/meeting/create
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<MeetingResponse> createMeeting(@RequestBody MeetingRequest request) {
        try {
            String meetingLink = calendarService.createMeeting(
                    request.getTitle(),
                    request.getDescription(),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getUserEmail(),
                    request.getAgentEmail());

            MeetingResponse response = new MeetingResponse();
            response.setMeetingLink(meetingLink);
            response.setTitle(request.getTitle());
            response.setStartTime(request.getStartTime());
            response.setEndTime(request.getEndTime());
            response.setStatus("CREATED");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create meeting: " + e.getMessage());
        }
    }

    /**
     * Get meeting details for an appointment
     * GET /api/meeting/{appointmentId}
     */
    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    public ResponseEntity<MeetingResponse> getMeeting(@PathVariable long appointmentId) {
        Booking booking = bookingRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Appointment not found"));

        MeetingResponse response = new MeetingResponse();
        response.setAppointmentId(appointmentId);
        response.setMeetingLink(booking.getMeetingLink());
        response.setTitle("Consultation with " + booking.getAgent().getName());
        response.setStartTime(booking.getStartTime().toString());
        response.setEndTime(booking.getEndTime().toString());
        response.setStatus(booking.getStatus());
        response.setUserEmail(booking.getUser().getEmail());
        response.setAgentEmail(booking.getAgent().getEmail());

        return ResponseEntity.ok(response);
    }

    /**
     * Validate meeting link
     * GET /api/meeting/validate
     */
    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    public ResponseEntity<ValidationResponse> validateMeetingLink(@RequestParam String meetingLink) {
        ValidationResponse response = new ValidationResponse();

        // Basic URL validation
        boolean isValid = meetingLink != null &&
                (meetingLink.startsWith("https://meet.google.com/") ||
                        meetingLink.startsWith("https://zoom.us/") ||
                        meetingLink.startsWith("https://teams.microsoft.com/"));

        response.setValid(isValid);
        response.setMeetingLink(meetingLink);

        if (!isValid) {
            response.setMessage("Invalid meeting link format");
        } else {
            response.setMessage("Valid meeting link");
        }

        return ResponseEntity.ok(response);
    }

    // DTO Classes
    public static class MeetingRequest {
        private String title;
        private String description;
        private String startTime;
        private String endTime;
        private String userEmail;
        private String agentEmail;

        // Getters and Setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public String getAgentEmail() {
            return agentEmail;
        }

        public void setAgentEmail(String agentEmail) {
            this.agentEmail = agentEmail;
        }
    }

    public static class MeetingResponse {
        private Long appointmentId;
        private String meetingLink;
        private String title;
        private String startTime;
        private String endTime;
        private String status;
        private String userEmail;
        private String agentEmail;

        // Getters and Setters
        public Long getAppointmentId() {
            return appointmentId;
        }

        public void setAppointmentId(Long appointmentId) {
            this.appointmentId = appointmentId;
        }

        public String getMeetingLink() {
            return meetingLink;
        }

        public void setMeetingLink(String meetingLink) {
            this.meetingLink = meetingLink;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public String getAgentEmail() {
            return agentEmail;
        }

        public void setAgentEmail(String agentEmail) {
            this.agentEmail = agentEmail;
        }
    }

    public static class ValidationResponse {
        private boolean valid;
        private String meetingLink;
        private String message;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMeetingLink() {
            return meetingLink;
        }

        public void setMeetingLink(String meetingLink) {
            this.meetingLink = meetingLink;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
