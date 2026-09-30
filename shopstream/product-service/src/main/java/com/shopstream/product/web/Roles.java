package com.shopstream.product.web;

import org.springframework.http.HttpStatus;

/**
 * Authorization check. The gateway already proved WHO the caller is (authentication);
 * each service decides WHAT they may do (authorization).
 */
public final class Roles {

    private Roles() {
    }

    public static void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
