package com.insurai.controller;

import com.insurai.service.TestDataGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    private final TestDataGeneratorService testDataGeneratorService;

    public TestController(TestDataGeneratorService testDataGeneratorService) {
        this.testDataGeneratorService = testDataGeneratorService;
    }

    /**
     * Trigger large-scale test data generation.
     * Restricted to SUPER_ADMIN authority.
     */
    @PostMapping("/seed")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> seedData() {
        try {
            long startTime = System.currentTimeMillis();
            testDataGeneratorService.generateTestData();
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Test data generated successfully!");
            response.put("duration_ms", duration);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Data generation failed: ", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Data generation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
