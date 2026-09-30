package com.shopstream.user.auth;

import com.shopstream.user.config.JwtProperties;
import com.shopstream.user.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Issues JSON Web Tokens.
 *
 * A JWT has three parts: header.payload.signature. The payload ("claims") holds
 * the user id, email and role. The signature is an HMAC-SHA256 of the first two
 * parts using a secret key that only user-service and the api-gateway know,
 * so nobody else can forge or modify a token.
 *
 * Paste a token into https://jwt.io to see what is inside it.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(JwtProperties properties) {
        // HS256 needs a key of at least 256 bits (32 bytes); Keys.hmacShaKeyFor enforces that.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = properties.expirationMinutes();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .issuer("shopstream-user-service")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
