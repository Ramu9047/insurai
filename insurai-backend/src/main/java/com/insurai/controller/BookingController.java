package com.insurai.controller;

import com.insurai.dto.BookingRequest;
import com.insurai.model.Booking;
import com.insurai.service.BookingService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:3000")
public class BookingController {

    private final BookingService bookingService;
    private final com.insurai.repository.UserRepository userRepo;

    public BookingController(BookingService bookingService, com.insurai.repository.UserRepository userRepo) {
        this.bookingService = bookingService;
        this.userRepo = userRepo;
    }

    // Access rule: Authenticated users can view their relevant bookings
    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public List<Booking> getAll(org.springframework.security.core.Authentication auth) {
        com.insurai.model.User user = null;
        if (auth != null && auth.isAuthenticated()) {
            String email = auth.getName();
            user = userRepo.findByEmail(email).orElse(null);
        }
        return bookingService.getAllBookings(user);
    }

    // Access rule: USER can create booking for themselves; SUPER_ADMIN can create for anyone
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('USER','SUPER_ADMIN')")
    public Booking create(@RequestBody BookingRequest request, org.springframework.security.core.Authentication auth) {
        Long userId = request.getUserId();
        Long agentId = request.getAgentId();

        if (userId == null || agentId == null) {
            throw new IllegalArgumentException("User ID and Agent ID are required");
        }

        if (auth != null && auth.isAuthenticated()) {
            String email = auth.getName();
            com.insurai.model.User currentUser = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (!currentUser.getId().equals(userId)
                    && !"SUPER_ADMIN".equals(currentUser.getRole())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Cannot book for another user");
            }
        }

        return bookingService.createBooking(
                userId,
                agentId,
                request.getStart(),
                request.getEnd(),
                request.getPolicyId(),
                request.getReason(),
                request.getBookingType());
    }

    // Access rule: USER can view their own bookings; ADMIN can view any user's bookings
    @GetMapping("/user/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public List<Booking> userBookings(@PathVariable Long id, org.springframework.security.core.Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            String email = auth.getName();
            com.insurai.model.User currentUser = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            boolean isAdmin = "SUPER_ADMIN".equals(currentUser.getRole())
                    || "COMPANY_ADMIN".equals(currentUser.getRole());
            if (!currentUser.getId().equals(id) && !isAdmin) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        return bookingService.getUserBookings(java.util.Objects.requireNonNull(id));
    }

    // Access rule: AGENT can view their assigned bookings; ADMIN can view any agent's bookings
    @GetMapping("/agent/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','COMPANY_ADMIN','SUPER_ADMIN')")
    public List<Booking> agentBookings(@PathVariable Long id, org.springframework.security.core.Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            String email = auth.getName();
            com.insurai.model.User currentUser = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            boolean isAdmin = "SUPER_ADMIN".equals(currentUser.getRole())
                    || "COMPANY_ADMIN".equals(currentUser.getRole());
            if (!currentUser.getId().equals(id) && !isAdmin) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        return bookingService.getAgentBookings(java.util.Objects.requireNonNull(id));
    }

    // Access rule: Only AGENT, COMPANY_ADMIN, or SUPER_ADMIN can update booking status
    @PutMapping("/{id}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','COMPANY_ADMIN','SUPER_ADMIN')")
    public Booking updateStatus(@PathVariable Long id,
            @RequestParam String status) {
        return bookingService.updateStatus(java.util.Objects.requireNonNull(id), status);
    }

    // Access rule: Only AGENT, COMPANY_ADMIN, or SUPER_ADMIN can patch booking status
    @PatchMapping("/{id}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','COMPANY_ADMIN','SUPER_ADMIN')")
    public Booking updateStatusPatch(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null)
            throw new IllegalArgumentException("Status is required in body");
        return bookingService.updateStatus(java.util.Objects.requireNonNull(id), status);
    }

    // Access rule: AGENT or SUPER_ADMIN can block a schedule slot
    @PostMapping("/block")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','SUPER_ADMIN')")
    public void blockSlot(@RequestBody BookingRequest request) {
        Long agentId = request.getAgentId();
        if (agentId == null) {
            throw new IllegalArgumentException("Agent ID is required");
        }
        bookingService.blockSlot(agentId, request.getStart(), request.getEnd());
    }

    // Access rule: Authenticated participants (booking user, assigned agent, or admin) can reschedule booking
    @PutMapping("/{id}/reschedule")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public Booking reschedule(
            @PathVariable Long id,
            @RequestBody BookingRequest request,
            @com.insurai.security.CurrentUser com.insurai.model.User currentUser) {
        if (currentUser == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Booking booking = bookingService.getBookingById(id);
        boolean isAdmin = currentUser.getRole() != null &&
                ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()));
        boolean isUserOwner = booking.getUser() != null && currentUser.getId().equals(booking.getUser().getId());
        boolean isAgentOwner = booking.getAgent() != null && currentUser.getId().equals(booking.getAgent().getId());

        if (!isAdmin && !isUserOwner && !isAgentOwner) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: You are not a participant in this booking");
        }

        return bookingService.rescheduleBooking(id, request.getStart(), request.getEnd());
    }

    // Access rule: Authenticated users can check slot availability
    @GetMapping("/availability")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public List<String> getAvailability(@RequestParam String date, @RequestParam Long agentId) {
        return bookingService.getAvailableSlots(date, agentId);
    }
}
