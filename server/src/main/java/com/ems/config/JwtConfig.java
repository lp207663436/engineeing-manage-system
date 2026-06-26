package com.ems.config;

import com.ems.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    @Value("${ems.jwt.secret}") private String secret;
    @Value("${ems.jwt.expire}") private long expire;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(secret, expire);
    }
}
