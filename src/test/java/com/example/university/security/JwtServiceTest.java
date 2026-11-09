package com.example.university.security;

import com.example.university.user.Role;
import com.example.university.user.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("0123456789abcdef0123456789abcdef", 60_000);

    @Test
    void generateTokenEmbedsUserClaimsAndExpiration() {
        User user = new User(
                "student.one",
                "hash",
                "Student One",
                "student.one@university.example",
                Role.STUDENT
        );
        ReflectionTestUtils.setField(user, "id", 42L);

        String token = jwtService.generateToken(user);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("student.one");
        assertThat(((Number) claims.get("uid")).longValue()).isEqualTo(42L);
        assertThat(claims.get("role", String.class)).isEqualTo("STUDENT");
        assertThat(((Number) claims.get("ver")).longValue()).isZero();
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(60);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}
