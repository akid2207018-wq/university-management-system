package com.example.university.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccountService userAccountService;

    @Test
    void createNormalizesUsernameAndEmailAndEncodesPassword() {
        UserWriteRequest request = new UserWriteRequest(
                " New.Student ",
                "Secret123!",
                " New Student ",
                " NEW.STUDENT@UNIVERSITY.EXAMPLE ",
                null
        );
        when(userRepository.existsByUsername("new.student")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userAccountService.create(Role.STUDENT, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("new.student");
        assertThat(saved.getEmail()).isEqualTo("new.student@university.example");
        assertThat(saved.getFullName()).isEqualTo("New Student");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(response.username()).isEqualTo("new.student");
        assertThat(response.role()).isEqualTo(Role.STUDENT);
    }

    @Test
    void createRejectsDuplicateUsername() {
        UserWriteRequest request = new UserWriteRequest(
                "admin",
                "Secret123!",
                "Admin",
                "admin@university.example",
                true
        );
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userAccountService.create(Role.ADMIN, request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("admin");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createRejectsBlankPassword() {
        UserWriteRequest request = new UserWriteRequest(
                "new.user",
                "   ",
                "New User",
                "new.user@university.example",
                true
        );

        assertThatThrownBy(() -> userAccountService.create(Role.STUDENT, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Password is required");
        verify(userRepository, never()).existsByUsername(anyString());
    }

    @Test
    void updateIncrementsTokenVersionWhenPasswordChanges() {
        User existing = new User(
                "teacher.user",
                "old-hash",
                "Teacher User",
                "teacher.user@university.example",
                Role.TEACHER
        );
        when(userRepository.findByIdAndRole(7L, Role.TEACHER)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("NewSecret123!")).thenReturn("new-hash");

        userAccountService.update(Role.TEACHER, 7L, new UserWriteRequest(
                "Teacher.User",
                "NewSecret123!",
                "Teacher User",
                "TEACHER.USER@UNIVERSITY.EXAMPLE",
                null
        ));

        assertThat(existing.getUsername()).isEqualTo("teacher.user");
        assertThat(existing.getEmail()).isEqualTo("teacher.user@university.example");
        assertThat(existing.getPassword()).isEqualTo("new-hash");
        assertThat(existing.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void getByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAccountService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
