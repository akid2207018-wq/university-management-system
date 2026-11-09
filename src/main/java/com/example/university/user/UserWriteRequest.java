package com.example.university.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserWriteRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "Username may only contain letters, numbers, dots, underscores, and hyphens"
        )
        String username,

        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        Boolean enabled
) {
}
