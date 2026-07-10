package com.gateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils("my-test-secret-key-must-be-at-least-32-characters-long", 3600000);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        String token = jwtUtils.generateToken("demo");

        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtils.generateToken("demo-user");

        String username = jwtUtils.extractUsername(token);

        assertEquals("demo-user", username);
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        assertFalse(jwtUtils.validateToken("invalid-token"));
    }

    @Test
    void validateToken_withNullToken_shouldReturnFalse() {
        assertFalse(jwtUtils.validateToken(null));
    }

    @Test
    void validateToken_withDifferentSecret_shouldReturnFalse() {
        String token = jwtUtils.generateToken("demo");

        JwtUtils otherJwtUtils = new JwtUtils("another-test-secret-key-must-be-at-least-32-characters", 3600000);

        assertFalse(otherJwtUtils.validateToken(token));
    }
}
