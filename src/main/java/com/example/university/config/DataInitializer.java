package com.example.university.config;

import com.example.university.user.Role;
import com.example.university.user.User;
import com.example.university.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;
    @Value("${app.bootstrap.admin.password:ChangeMe123!}")
    private String adminPassword;
    @Value("${app.bootstrap.admin.full-name:University Administrator}")
    private String adminFullName;
    @Value("${app.bootstrap.admin.email:admin@university.example}")
    private String adminEmail;

    @Value("${app.bootstrap.teacher.username:teacher}")
    private String teacherUsername;
    @Value("${app.bootstrap.teacher.password:ChangeMe123!}")
    private String teacherPassword;
    @Value("${app.bootstrap.teacher.full-name:Sample Teacher}")
    private String teacherFullName;
    @Value("${app.bootstrap.teacher.email:teacher@university.example}")
    private String teacherEmail;

    @Value("${app.bootstrap.student.username:student}")
    private String studentUsername;
    @Value("${app.bootstrap.student.password:ChangeMe123!}")
    private String studentPassword;
    @Value("${app.bootstrap.student.full-name:Sample Student}")
    private String studentFullName;
    @Value("${app.bootstrap.student.email:student@university.example}")
    private String studentEmail;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        createIfMissing(adminUsername, adminPassword, adminFullName, adminEmail, Role.ADMIN);
        createIfMissing(teacherUsername, teacherPassword, teacherFullName, teacherEmail, Role.TEACHER);
        createIfMissing(studentUsername, studentPassword, studentFullName, studentEmail, Role.STUDENT);
    }

    private void createIfMissing(
            String username,
            String password,
            String fullName,
            String email,
            Role role
    ) {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(normalizedUsername)) {
            return;
        }
        if (password == null || password.length() < 8) {
            throw new IllegalStateException("Bootstrap password for " + role + " must be at least 8 characters");
        }

        User user = new User(
                normalizedUsername,
                passwordEncoder.encode(password),
                fullName.trim(),
                email.trim().toLowerCase(Locale.ROOT),
                role
        );
        userRepository.save(user);
        log.info("Created bootstrap {} account '{}'", role, normalizedUsername);
    }
}
