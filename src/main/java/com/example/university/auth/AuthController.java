package com.example.university.auth;

import com.example.university.security.JwtService;
import com.example.university.user.ResourceNotFoundException;
import com.example.university.user.User;
import com.example.university.user.UserAccountService;
import com.example.university.user.UserRepository;
import com.example.university.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserAccountService userAccountService;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            UserAccountService userAccountService,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username().trim().toLowerCase(Locale.ROOT),
                            request.password()
                    )
            );
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));

        LoginResponse response = new LoginResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                UserResponse.from(user)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        userAccountService.revokeTokens(principal.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userAccountService.getById(principal.id()));
    }
}
