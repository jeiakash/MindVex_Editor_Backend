package ai.mindvex.backend.controller;

import ai.mindvex.backend.dto.UserResponse;
import ai.mindvex.backend.entity.User;
import ai.mindvex.backend.repository.UserRepository;
import ai.mindvex.backend.security.JwtAuthenticationFilter;
import ai.mindvex.backend.security.JwtService;
import ai.mindvex.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestJpaConfig.class)
@DisplayName("UserController Slice Tests")
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        private User testUser;
        private Authentication mockAuth;
        private final Long USER_ID = 1L;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(USER_ID).email("test@example.com")
                                .fullName("Test User").provider("local").build();
                when(userRepository.findByEmail("user")).thenReturn(Optional.of(testUser));

                org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                                "user", "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
                mockAuth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        }

        @Test
        @DisplayName("GET /api/users/me: returns current user details")
        void whenGetMe_thenReturnsUser() throws Exception {
                UserResponse userResponse = UserResponse.builder()
                                .id(USER_ID).email("test@example.com")
                                .fullName("Test User").createdAt(LocalDateTime.now()).build();

                when(userService.getCurrentUser("user")).thenReturn(userResponse);

                mockMvc.perform(get("/api/users/me").principal(mockAuth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        @DisplayName("GET /api/users/me/github-connection: returns connection status")
        void whenGetGithubConnection_thenReturnsStatus() throws Exception {
                mockMvc.perform(get("/api/users/me/github-connection").principal(mockAuth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.connected").exists());
        }

        @Test
        @DisplayName("DELETE /api/users/me/github-connection: disconnects GitHub")
        void whenDisconnectGithub_thenReturns204() throws Exception {
                mockMvc.perform(delete("/api/users/me/github-connection").principal(mockAuth))
                                .andExpect(status().isNoContent());
        }
}
