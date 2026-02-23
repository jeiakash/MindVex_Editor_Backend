package ai.mindvex.backend.repository;

import ai.mindvex.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("encoded_password")
                .fullName("Test User")
                .provider("local")
                .createdAt(now)
                .updatedAt(now)
                .build();

        entityManager.persistAndFlush(testUser);
    }

    @Test
    @DisplayName("findByEmail: when user exists, then returns user")
    void whenUserExists_thenReturnUser() {
        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("findByEmail: when user not found, then returns empty")
    void whenUserNotFound_thenReturnEmpty() {
        Optional<User> found = userRepository.findByEmail("unknown@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail: when email exists, then returns true")
    void whenEmailExists_thenReturnTrue() {
        boolean exists = userRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail: when email not found, then returns false")
    void whenEmailNotFound_thenReturnFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByProviderAndProviderId: when matches, then returns user")
    void whenProviderMatches_thenReturnUser() {
        LocalDateTime now = LocalDateTime.now();
        User githubUser = User.builder()
                .email("github@example.com")
                .fullName("GitHub User")
                .provider("github")
                .providerId("gh_12345")
                .createdAt(now)
                .updatedAt(now)
                .build();
        entityManager.persistAndFlush(githubUser);

        Optional<User> found = userRepository.findByProviderAndProviderId("github", "gh_12345");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("github@example.com");
    }

    @Test
    @DisplayName("findByProviderAndProviderId: when no match, then returns empty")
    void whenProviderNotFound_thenReturnEmpty() {
        Optional<User> found = userRepository.findByProviderAndProviderId("github", "nonexistent");

        assertThat(found).isEmpty();
    }
}
