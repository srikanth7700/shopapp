package com.shopstream.user.auth;

import com.shopstream.user.config.JwtProperties;
import com.shopstream.user.user.Role;
import com.shopstream.user.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

    @Test
    void tokenContainsUserIdEmailAndRole() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 30));
        User user = new User("admin@shopstream.dev", "hash", "Admin", Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", 42L);

        String token = jwtService.generateToken(user);

        // This is the same check the api-gateway performs on every request.
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("admin@shopstream.dev");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(1800);
    }

    @Test
    void tokenContainsNameIssuerAndConfiguredExpiration() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 2));
        User user = new User("customer@shopstream.dev", "hash", "ShopStream Customer", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 73L);
        Instant beforeGeneration = Instant.now();

        String token = jwtService.generateToken(user);
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Instant expiration = claims.getExpiration().toInstant();

        assertThat(claims.get("name", String.class)).isEqualTo("ShopStream Customer");
        assertThat(claims.getIssuer()).isEqualTo("shopstream-user-service");
        assertThat(expiration).isAfterOrEqualTo(beforeGeneration.plusSeconds(120));
        assertThat(expiration).isBeforeOrEqualTo(Instant.now().plusSeconds(120));
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(120);
    }

    @Test
    void constructorRejectsSecretShorterThanHs256KeyRequirement() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 30)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
