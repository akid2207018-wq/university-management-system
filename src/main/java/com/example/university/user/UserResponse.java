package com.example.university.user;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
