package com.shopstream.user.user;

/** What the API returns about a user. Never expose the entity (it has the password hash). */
public record UserResponse(Long id, String email, String fullName, String role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
