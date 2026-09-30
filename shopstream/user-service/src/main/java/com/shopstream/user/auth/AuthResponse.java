package com.shopstream.user.auth;

import com.shopstream.user.user.UserResponse;

public record AuthResponse(String token, long expiresInSeconds, UserResponse user) {
}
