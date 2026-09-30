package com.shopstream.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Runs for every request that matches a route.
 *
 * 1. Removes any X-User-* headers the caller sent (so nobody can pretend to be another user).
 * 2. Lets public endpoints through (login, register, browsing products and stock levels).
 * 3. For everything else, validates the "Authorization: Bearer <jwt>" header.
 * 4. Adds X-User-Id / X-User-Email / X-User-Role headers for the downstream services.
 *
 * This is the "authenticate at the edge" pattern: the microservices behind the
 * gateway trust these headers and stay simple. That is only safe because the
 * services are not reachable from outside (private Docker network / Kubernetes
 * ClusterIP services). A stricter "zero trust" setup would re-validate the JWT
 * in every service too.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private static final String BEARER_PREFIX = "Bearer ";

    private record PublicEndpoint(HttpMethod method, String pattern) {
    }

    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint(HttpMethod.POST, "/api/auth/**"),
            new PublicEndpoint(HttpMethod.GET, "/api/products"),
            new PublicEndpoint(HttpMethod.GET, "/api/products/**"),
            // Stock levels are public so the product page can show "Only 2 left".
            new PublicEndpoint(HttpMethod.GET, "/api/inventory"),
            new PublicEndpoint(HttpMethod.GET, "/api/inventory/**"));

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        ServerHttpRequest.Builder builder = request.mutate().headers(headers -> {
            headers.remove(USER_ID_HEADER);
            headers.remove(USER_EMAIL_HEADER);
            headers.remove(USER_ROLE_HEADER);  
            
        });

        if (HttpMethod.OPTIONS.equals(request.getMethod()) || isPublic(request)) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "Missing bearer token");
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authHeader.substring(BEARER_PREFIX.length()))
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            // Covers bad signature, expired token, malformed token.
            return unauthorized(exchange, "Invalid or expired token");
        }

        ServerHttpRequest authenticated = builder
                .header(USER_ID_HEADER, claims.getSubject())
                .header(USER_EMAIL_HEADER, String.valueOf(claims.get("email", String.class)))
                .header(USER_ROLE_HEADER, String.valueOf(claims.get("role", String.class)))
                .build();

        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getPath().value();
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(endpoint -> endpoint.method().equals(request.getMethod())
                        && pathMatcher.match(endpoint.pattern(), path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"" + message + "\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    /** Run before the routing filters (which have the lowest precedence). */
    @Override
    public int getOrder() {
        return -100;
    }
}
