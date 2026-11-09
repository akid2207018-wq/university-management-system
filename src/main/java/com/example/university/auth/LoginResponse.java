package com.example.university.auth;

import com.example.university.user.UserResponse;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
