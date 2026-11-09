package com.example.university.auth;

import com.example.university.user.Role;

public record UserPrincipal(Long id, String username, Role role) {
}
