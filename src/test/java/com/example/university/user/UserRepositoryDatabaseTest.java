package com.example.university.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Tag("database")
class UserRepositoryDatabaseTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndFindByUsernameRoundTripsPersistedData() {
        User saved = userRepository.saveAndFlush(newUser("db.student", Role.STUDENT));
        entityManager.clear();

        User found = userRepository.findByUsername("db.student").orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getRole()).isEqualTo(Role.STUDENT);
        assertThat(found.getPassword()).isEqualTo("hash");
        assertThat(found.isEnabled()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void usernameUniqueConstraintIsEnforcedByTheDatabase() {
        userRepository.saveAndFlush(newUser("duplicate.user", Role.STUDENT));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("duplicate.user", Role.TEACHER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllByRoleFiltersAndOrdersByIdAscending() {
        User firstStudent = userRepository.save(newUser("db.student.one", Role.STUDENT));
        userRepository.save(newUser("db.teacher", Role.TEACHER));
        User secondStudent = userRepository.save(newUser("db.student.two", Role.STUDENT));
        userRepository.flush();

        List<User> students = userRepository.findAllByRoleOrderByIdAsc(Role.STUDENT);
        List<User> teachers = userRepository.findAllByRoleOrderByIdAsc(Role.TEACHER);

        assertThat(students).extracting(User::getUsername)
                .containsExactly("db.student.one", "db.student.two");
        assertThat(teachers).extracting(User::getUsername)
                .containsExactly("db.teacher");
        assertThat(firstStudent.getId()).isLessThan(secondStudent.getId());
    }

    @Test
    void findByIdAndRoleReturnsEmptyWhenRoleDoesNotMatch() {
        User teacher = userRepository.saveAndFlush(newUser("role.check", Role.TEACHER));

        assertThat(userRepository.findByIdAndRole(teacher.getId(), Role.TEACHER)).isPresent();
        assertThat(userRepository.findByIdAndRole(teacher.getId(), Role.STUDENT)).isEmpty();
    }

    private User newUser(String username, Role role) {
        return new User(
                username,
                "hash",
                "Test " + role.name().toLowerCase(),
                username + "@university.example",
                role
        );
    }
}
