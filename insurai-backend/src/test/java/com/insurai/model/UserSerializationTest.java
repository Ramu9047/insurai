package com.insurai.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testUserSerializationExcludesSensitiveFields() throws Exception {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("secretPassword123");
        user.setResetToken("secretResetToken456");
        user.setVerificationToken("secretVerificationToken789");

        String json = objectMapper.writeValueAsString(user);

        assertFalse(json.contains("password"), "Serialized User JSON should not contain password field");
        assertFalse(json.contains("resetToken"), "Serialized User JSON should not contain resetToken field");
        assertFalse(json.contains("verificationToken"), "Serialized User JSON should not contain verificationToken field");
        assertFalse(json.contains("secretPassword123"), "Serialized User JSON should not contain password value");
        assertFalse(json.contains("secretResetToken456"), "Serialized User JSON should not contain resetToken value");
    }
}
