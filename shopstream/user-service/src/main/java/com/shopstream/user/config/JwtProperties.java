package com.shopstream.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code app.jwt.*} settings from application.yml into a typed object.
 * Type-safe config beats sprinkling {@code @Value("${...}")} everywhere.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
