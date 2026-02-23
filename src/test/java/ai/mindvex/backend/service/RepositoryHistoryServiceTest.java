package ai.mindvex.backend.service;

import ai.mindvex.backend.dto.RepositoryHistoryRequest;
import ai.mindvex.backend.dto.RepositoryHistoryResponse;
import ai.mindvex.backend.entity.RepositoryHistory;
import ai.mindvex.backend.repository.RepositoryHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepositoryHistoryService Unit Tests")
class RepositoryHistoryServiceTest {

        @Mock
        private RepositoryHistoryRepository repositoryHistoryRepository;

        @InjectMocks
        private RepositoryHistoryService repositoryHistoryService;

        private RepositoryHistory testRepo;
        private RepositoryHistoryRequest testRequest;
        private final Long USER_ID = 1L;
        private final Long REPO_ID = 100L;

        @BeforeEach
        void setUp() {
                testRepo = RepositoryHistory.builder()
                                .id(REPO_ID)
                                .userId(USER_ID)
                                .url("https://github.com/user/repo")
                                .name("TestRepo")
                                .description("Test repository")
                                .branch("main")
                                .commitHash("abc123")
                                .createdAt(LocalDateTime.now())
                                .lastAccessedAt(LocalDateTime.now())
                                .build();

                testRequest = RepositoryHistoryRequest.builder()
                                .url("https://github.com/user/repo")
                                .name("TestRepo")
                                .description("Test repository")
                                .branch("main")
                                .commitHash("abc123")
                                .build();
        }

        // ========== GET HISTORY ==========

        @Test
        @DisplayName("getRepositoryHistory: when user has repos, then returns ordered list")
        void whenUserHasRepos_thenReturnOrderedList() {
                when(repositoryHistoryRepository.findByUserIdOrderByLastAccessedAtDesc(USER_ID))
                                .thenReturn(List.of(testRepo));

                List<RepositoryHistoryResponse> result = repositoryHistoryService.getRepositoryHistory(USER_ID);

                assertEquals(1, result.size());
                assertEquals("TestRepo", result.get(0).getName());
        }

        @Test
        @DisplayName("getRepositoryHistory with limit: when called, then uses pagination")
        void whenCalledWithLimit_thenUsePagination() {
                when(repositoryHistoryRepository.findByUserId(eq(USER_ID), any(PageRequest.class)))
                                .thenReturn(List.of(testRepo));

                List<RepositoryHistoryResponse> result = repositoryHistoryService.getRepositoryHistory(USER_ID, 10);

                assertEquals(1, result.size());
                verify(repositoryHistoryRepository).findByUserId(eq(USER_ID), any(PageRequest.class));
        }

        // ========== ADD REPOSITORY ==========

        @Test
        @DisplayName("addRepository: when new repo, then creates new entry")
        void whenNewRepo_thenCreateNewEntry() {
                when(repositoryHistoryRepository.findByUserIdAndUrl(USER_ID, testRequest.getUrl()))
                                .thenReturn(Optional.empty());
                when(repositoryHistoryRepository.countByUserId(USER_ID)).thenReturn(0L);
                when(repositoryHistoryRepository.save(any(RepositoryHistory.class))).thenReturn(testRepo);

                RepositoryHistoryResponse response = repositoryHistoryService.addRepository(USER_ID, testRequest);

                assertNotNull(response);
                assertEquals("TestRepo", response.getName());
                verify(repositoryHistoryRepository).save(any(RepositoryHistory.class));
        }

        @Test
        @DisplayName("addRepository: when existing repo, then updates entry")
        void whenExistingRepo_thenUpdateEntry() {
                when(repositoryHistoryRepository.findByUserIdAndUrl(USER_ID, testRequest.getUrl()))
                                .thenReturn(Optional.of(testRepo));
                when(repositoryHistoryRepository.save(any(RepositoryHistory.class))).thenReturn(testRepo);

                RepositoryHistoryResponse response = repositoryHistoryService.addRepository(USER_ID, testRequest);

                assertNotNull(response);
                verify(repositoryHistoryRepository).save(testRepo);
                // Should NOT check count since it's an update
                verify(repositoryHistoryRepository, never()).countByUserId(anyLong());
        }

        @Test
        @DisplayName("addRepository: when at max limit, then evicts oldest entry")
        void whenAtMaxLimit_thenEvictsOldest() {
                RepositoryHistory oldRepo = RepositoryHistory.builder()
                                .id(1L)
                                .userId(USER_ID)
                                .url("https://github.com/old/repo")
                                .name("OldRepo")
                                .createdAt(LocalDateTime.now().minusDays(30))
                                .lastAccessedAt(LocalDateTime.now().minusDays(30))
                                .build();

                RepositoryHistoryRequest newRequest = RepositoryHistoryRequest.builder()
                                .url("https://github.com/new/repo")
                                .name("NewRepo")
                                .build();

                when(repositoryHistoryRepository.findByUserIdAndUrl(USER_ID, newRequest.getUrl()))
                                .thenReturn(Optional.empty());
                when(repositoryHistoryRepository.countByUserId(USER_ID)).thenReturn(50L);
                when(repositoryHistoryRepository.findOldestByUserId(eq(USER_ID), any(PageRequest.class)))
                                .thenReturn(List.of(oldRepo));
                when(repositoryHistoryRepository.save(any(RepositoryHistory.class))).thenReturn(testRepo);

                repositoryHistoryService.addRepository(USER_ID, newRequest);

                verify(repositoryHistoryRepository).delete(oldRepo);
                verify(repositoryHistoryRepository).save(any(RepositoryHistory.class));
        }

        // ========== REMOVE REPOSITORY ==========

        @Test
        @DisplayName("removeRepository: when repo exists, then deletes successfully")
        void whenRepoExists_thenDeleteSuccessfully() {
                when(repositoryHistoryRepository.findByIdAndUserId(REPO_ID, USER_ID))
                                .thenReturn(Optional.of(testRepo));
                doNothing().when(repositoryHistoryRepository).delete(testRepo);

                assertDoesNotThrow(() -> repositoryHistoryService.removeRepository(USER_ID, REPO_ID));
                verify(repositoryHistoryRepository).delete(testRepo);
        }

        @Test
        @DisplayName("removeRepository: when repo not found, then throws IllegalArgumentException")
        void whenRepoNotFound_thenThrowException() {
                when(repositoryHistoryRepository.findByIdAndUserId(REPO_ID, USER_ID))
                                .thenReturn(Optional.empty());

                assertThrows(IllegalArgumentException.class,
                                () -> repositoryHistoryService.removeRepository(USER_ID, REPO_ID));
        }

        // ========== CLEAR HISTORY ==========

        @Test
        @DisplayName("clearHistory: when called, then deletes all for user")
        void whenClearHistory_thenDeleteAll() {
                doNothing().when(repositoryHistoryRepository).deleteAllByUserId(USER_ID);

                assertDoesNotThrow(() -> repositoryHistoryService.clearHistory(USER_ID));
                verify(repositoryHistoryRepository).deleteAllByUserId(USER_ID);
        }
}
