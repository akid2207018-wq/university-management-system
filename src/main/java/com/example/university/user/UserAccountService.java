package com.example.university.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> list(Role role) {
        return userRepository.findAllByRoleOrderByIdAsc(role).stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse get(Role role, Long id) {
        return UserResponse.from(find(role, id));
    }

    public UserResponse getById(Long id) {
        return UserResponse.from(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " was not found")));
    }

    @Transactional
    public UserResponse create(Role role, UserWriteRequest request) {
        String username = normalizeUsername(request.username());
        if (request.password() == null || request.password().isBlank()) {
            throw new InvalidRequestException("Password is required when creating a user");
        }
        validatePasswordLength(request.password());
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        User user = new User(
                username,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.email().trim().toLowerCase(Locale.ROOT),
                role
        );
        user.setEnabled(request.enabled() == null || request.enabled());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Role role, Long id, UserWriteRequest request) {
        User user = find(role, id);
        String username = normalizeUsername(request.username());
        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        user.setUsername(username);
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));

        if (request.password() != null) {
            if (request.password().isBlank()) {
                throw new InvalidRequestException("Password cannot be blank");
            }
            validatePasswordLength(request.password());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.incrementTokenVersion();
        }
        if (request.enabled() != null && request.enabled() != user.isEnabled()) {
            user.setEnabled(request.enabled());
            user.incrementTokenVersion();
        }

        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Role role, Long id) {
        User user = find(role, id);
        userRepository.delete(user);
    }

    @Transactional
    public void revokeTokens(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " was not found"));
        user.incrementTokenVersion();
    }

    private User find(Role role, Long id) {
        return userRepository.findByIdAndRole(id, role)
                .orElseThrow(() -> new ResourceNotFoundException(role.name().toLowerCase(Locale.ROOT) + " " + id + " was not found"));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("Password must be at most 72 bytes");
        }
    }
}
