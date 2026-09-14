package com.insurai.controller;

import com.insurai.model.User;
import com.insurai.repository.UserRepository;
import com.insurai.security.JwtTokenProvider;
import com.insurai.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final com.insurai.repository.CompanyRepository companyRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final com.insurai.service.NotificationService notificationService;

    public AuthController(UserRepository userRepository,
            com.insurai.repository.CompanyRepository companyRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            EmailService emailService,
            com.insurai.service.NotificationService notificationService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @jakarta.validation.Valid @RequestBody com.insurai.dto.RegisterRequest req,
            org.springframework.security.core.Authentication auth) {
        // Security: Block SuperAdmin creation
        if ("SUPER_ADMIN".equalsIgnoreCase(req.getRole())) {
            return ResponseEntity.status(403).body("Registration is restricted for this role.");
        }

        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());

        // Profile fields
        user.setAge(req.getAge());
        user.setPhone(req.getPhone());
        user.setIncome(req.getIncome());
        user.setDependents(req.getDependents());
        user.setHealthInfo(req.getHealthInfo());
        user.setAddress(req.getAddress());
        user.setSpecialization(req.getSpecialization());
        user.setBio(req.getBio());

        // Safe Defaults - Server enforced
        user.setAvailable(false);
        user.setVerified(false);
        user.setIsActive(true);

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);

        // Handle Company assignment
        if (req.getCompanyId() != null) {
            Long cId = req.getCompanyId();
            companyRepository.findById(java.util.Objects.requireNonNull(cId)).ifPresent(user::setCompany);
        } else if (auth != null && auth.isAuthenticated()) {
            String creatorEmail = auth.getName();
            userRepository.findByEmail(creatorEmail).ifPresent(currentUser -> {
                if (currentUser.getCompany() != null) {
                    user.setCompany(currentUser.getCompany());
                }
            });

            if (user.getCompany() == null) {
                companyRepository.findByEmail(creatorEmail).ifPresent(user::setCompany);
            }
        }

        User saved = userRepository.save(user);

        // Notify Admins for real-time dashboard update
        notificationService.broadcastUpdate("admin-updates", "NEW_USER_REGISTERED");

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@jakarta.validation.Valid @RequestBody com.insurai.dto.LoginRequest request) {
        // 1. Try User Login
        var userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(401).body("Invalid email or password");
            }

            if (Boolean.FALSE.equals(user.getIsActive())) {
                return ResponseEntity.status(403).body("Account is deactivated. Contact Admin.");
            }

            // Generate Token
            String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());

            // Auto-set Agent to Online
            if ("AGENT".equals(user.getRole())) {
                user.setAvailable(true);
                userRepository.save(user);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("token", token);

            return ResponseEntity.ok(response);
        }

        // 2. Try Company Login
        var companyOpt = companyRepository.findByEmail(request.getEmail());
        if (companyOpt.isPresent()) {
            com.insurai.model.Company company = companyOpt.get();
            if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), company.getPassword())) {
                return ResponseEntity.status(401).body("Invalid email or password");
            }

            if (Boolean.FALSE.equals(company.getIsActive())) {
                return ResponseEntity.status(403).body("Company account is deactivated. Contact Admin.");
            }

            // Generate Token
            String token = jwtTokenProvider.generateToken(company.getEmail(), "COMPANY", company.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("user", company);
            response.put("token", token);

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body("Invalid email or password");
    }

    @GetMapping("/forgot")
    public ResponseEntity<?> forgot(@RequestParam String email) {
        String responseMsg = "If that email exists, a reset link has been sent.";
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(responseMsg);
        }

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            String token = UUID.randomUUID().toString();
            u.setResetToken(token);
            u.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(30));
            userRepository.save(u);

            String link = "http://localhost:3000/reset-password?token=" + token;
            try {
                emailService.send(email, "Reset Your Password", "Click here to reset: " + link);
            } catch (Exception e) {
                logger.error("Email failed to send to {}: {}", email, e.getMessage());
            }
        }

        return ResponseEntity.ok(responseMsg);
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> payload) {
        String token = payload != null ? payload.get("token") : null;
        String newPassword = payload != null ? payload.get("newPassword") : null;

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Reset token is required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body("New password must be at least 8 characters");
        }

        User u = userRepository.findByResetToken(token)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid or expired token"));

        if (u.getResetTokenExpiry() == null || u.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Reset token has expired");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        u.setResetToken(null);
        u.setResetTokenExpiry(null);
        userRepository.save(u);

        return ResponseEntity.ok("Password updated successfully");
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> payload,
            org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        if (oldPassword == null || newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body("Invalid request: password must be at least 8 characters");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email).map(user -> {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ResponseEntity.status(400).body(Map.of("message", "Current password is incorrect"));
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        }).orElse(ResponseEntity.status(404).body(Map.of("message", "User not found")));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Verification token is required");
        }
        User u = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid or expired verification token"));
        u.setVerified(true);
        u.setVerificationToken(null);
        userRepository.save(u);
        return ResponseEntity.ok("Account verified successfully!");
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", System.currentTimeMillis()));
    }
}
