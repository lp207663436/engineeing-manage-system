package com.ems.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(
            "ems-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm", 86400L);

    @Test
    void generateAndParse_shouldReturnSameUserId() {
        String token = jwtUtil.generate(1L, "admin");
        assertEquals(1L, jwtUtil.parseUserId(token));
        assertEquals("admin", jwtUtil.parseUsername(token));
    }

    @Test
    void parseUserId_invalidToken_shouldReturnNull() {
        assertNull(jwtUtil.parseUserId("invalid.token.here"));
    }
}
