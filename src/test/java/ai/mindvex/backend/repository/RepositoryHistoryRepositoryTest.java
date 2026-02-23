package ai.mindvex.backend.repository;

import ai.mindvex.backend.entity.RepositoryHistory;
import ai.mindvex.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RepositoryHistoryRepository Integration Tests")
class RepositoryHistoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RepositoryHistoryRepository repositoryHistoryRepository;

    private RepositoryHistory repo1;
    private RepositoryHistory repo2;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("repotest@example.com")
                .passwordHash("encoded")
                .fullName("Repo Test User")
                .provider("local")
                .build();
        entityManager.persistAndFlush(testUser);

        repo1 = RepositoryHistory.builder()
                .userId(testUser.getId())
                .url("https://github.com/user/repo-a")
                .name("Repo A")
                .description("First repo")
                .branch("main")
                .createdAt(LocalDateTime.now().minusDays(10))
                .lastAccessedAt(LocalDateTime.now().minusDays(2))
                .build();

        repo2 = RepositoryHistory.builder()
                .userId(testUser.getId())
                .url("https://github.com/user/repo-b")
                .name("Repo B")
                .description("Second repo")
                .branch("develop")
                .createdAt(LocalDateTime.now().minusDays(5))
                .lastAccessedAt(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(repo1);
        entityManager.persistAndFlush(repo2);
    }

    @Test
    @DisplayName("findByUserIdOrderByLastAccessedAtDesc: returns repos ordered by latest first")
    void whenUserHasRepos_thenReturnOrderedByLatest() {
        List<RepositoryHistory> repos = repositoryHistoryRepository
                .findByUserIdOrderByLastAccessedAtDesc(testUser.getId());

        assertThat(repos).hasSize(2);
        assertThat(repos.get(0).getName()).isEqualTo("Repo B");
        assertThat(repos.get(1).getName()).isEqualTo("Repo A");
    }

    @Test
    @DisplayName("findByUserId with pagination: returns limited results")
    void whenCalledWithPagination_thenReturnsLimited() {
        List<RepositoryHistory> repos = repositoryHistoryRepository
                .findByUserId(testUser.getId(), PageRequest.of(0, 1));

        assertThat(repos).hasSize(1);
    }

    @Test
    @DisplayName("findByUserIdAndUrl: when repo exists, then returns it")
    void whenRepoExists_thenReturnIt() {
        Optional<RepositoryHistory> found = repositoryHistoryRepository
                .findByUserIdAndUrl(testUser.getId(), "https://github.com/user/repo-a");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Repo A");
    }

    @Test
    @DisplayName("findByUserIdAndUrl: when url not found, then returns empty")
    void whenUrlNotFound_thenReturnEmpty() {
        Optional<RepositoryHistory> found = repositoryHistoryRepository
                .findByUserIdAndUrl(testUser.getId(), "https://github.com/nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndUserId: when repo exists for user, then returns it")
    void whenRepoExistsForUser_thenReturnIt() {
        Optional<RepositoryHistory> found = repositoryHistoryRepository
                .findByIdAndUserId(repo1.getId(), testUser.getId());

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("findByIdAndUserId: when wrong user, then returns empty")
    void whenWrongUser_thenReturnEmpty() {
        Optional<RepositoryHistory> found = repositoryHistoryRepository
                .findByIdAndUserId(repo1.getId(), 999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("countByUserId: returns correct count")
    void whenCountByUserId_thenReturnCorrectCount() {
        long count = repositoryHistoryRepository.countByUserId(testUser.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByUserId: when no repos, then returns 0")
    void whenNoRepos_thenReturnZero() {
        long count = repositoryHistoryRepository.countByUserId(999L);

        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("deleteAllByUserId: removes all entries for user")
    void whenDeleteAllByUserId_thenRemovesAll() {
        repositoryHistoryRepository.deleteAllByUserId(testUser.getId());
        entityManager.flush();

        List<RepositoryHistory> remaining = repositoryHistoryRepository
                .findByUserIdOrderByLastAccessedAtDesc(testUser.getId());

        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("findOldestByUserId: returns oldest entries with pagination")
    void whenFindOldest_thenReturnsOldestFirst() {
        List<RepositoryHistory> oldest = repositoryHistoryRepository
                .findOldestByUserId(testUser.getId(), PageRequest.of(0, 1));

        assertThat(oldest).hasSize(1);
        assertThat(oldest.get(0).getName()).isEqualTo("Repo A");
    }
}
