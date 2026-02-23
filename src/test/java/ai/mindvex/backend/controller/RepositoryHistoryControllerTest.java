package ai.mindvex.backend.controller;

import ai.mindvex.backend.dto.RepositoryHistoryRequest;
import ai.mindvex.backend.dto.RepositoryHistoryResponse;
import ai.mindvex.backend.entity.User;
import ai.mindvex.backend.repository.UserRepository;
import ai.mindvex.backend.security.JwtAuthenticationFilter;
import ai.mindvex.backend.security.JwtService;
import ai.mindvex.backend.service.RepositoryHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RepositoryHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestJpaConfig.class)
@DisplayName("RepositoryHistoryController Slice Tests")
class RepositoryHistoryControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private RepositoryHistoryService repositoryHistoryService;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        private final Long USER_ID = 1L;
        private RepositoryHistoryResponse testResponse;
        private Authentication mockAuth;

        @BeforeEach
        void setUp() {
                User testUser = User.builder().id(USER_ID).email("test@example.com").fullName("Test User").build();
                when(userRepository.findByEmail("user")).thenReturn(Optional.of(testUser));

                org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                                "user", "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
                mockAuth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                testResponse = RepositoryHistoryResponse.builder()
                                .id(100L).url("https://github.com/user/repo").name("Test Repo")
                                .description("A test repository").branch("main")
                                .createdAt(LocalDateTime.now()).lastAccessedAt(LocalDateTime.now())
                                .build();
        }

        @Test
        @DisplayName("GET /api/repository-history: returns history list")
        void whenGetHistory_thenReturnList() throws Exception {
                when(repositoryHistoryService.getRepositoryHistory(USER_ID))
                                .thenReturn(List.of(testResponse));

                mockMvc.perform(get("/api/repository-history").principal(mockAuth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Test Repo"));
        }

        @Test
        @DisplayName("POST /api/repository-history: adds history entry returns 201")
        void whenAddHistory_thenReturns201() throws Exception {
                RepositoryHistoryRequest request = new RepositoryHistoryRequest();
                request.setUrl("https://github.com/user/repo");
                request.setName("Test Repo");

                when(repositoryHistoryService.addRepository(eq(USER_ID), any(RepositoryHistoryRequest.class)))
                                .thenReturn(testResponse);

                mockMvc.perform(post("/api/repository-history")
                                .principal(mockAuth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Test Repo"));
        }

        @Test
        @DisplayName("DELETE /api/repository-history/{id}: removes entry returns 204")
        void whenDeleteHistory_thenReturns204() throws Exception {
                mockMvc.perform(delete("/api/repository-history/100").principal(mockAuth))
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/repository-history: clears all history returns 204")
        void whenClearHistory_thenReturns204() throws Exception {
                mockMvc.perform(delete("/api/repository-history").principal(mockAuth))
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GET /api/repository-history: empty list when no history")
        void whenNoHistory_thenReturnEmptyList() throws Exception {
                when(repositoryHistoryService.getRepositoryHistory(USER_ID))
                                .thenReturn(List.of());

                mockMvc.perform(get("/api/repository-history").principal(mockAuth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());
        }
}
