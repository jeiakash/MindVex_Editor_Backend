package ai.mindvex.backend.controller;

import ai.mindvex.backend.dto.AuthResponse;
import ai.mindvex.backend.dto.LoginRequest;
import ai.mindvex.backend.dto.RegisterRequest;
import ai.mindvex.backend.dto.UserResponse;
import ai.mindvex.backend.security.JwtAuthenticationFilter;
import ai.mindvex.backend.security.JwtService;
import ai.mindvex.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestJpaConfig.class)
@DisplayName("AuthController Slice Tests")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private UserService userService;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Test
        @DisplayName("POST /api/auth/register: returns token on success")
        void whenRegister_thenReturnsToken() throws Exception {
                RegisterRequest request = new RegisterRequest();
                request.setEmail("test@example.com");
                request.setPassword("password123");
                request.setFullName("Test User");

                UserResponse userResp = UserResponse.builder()
                                .id(1L).email("test@example.com").fullName("Test User").build();

                AuthResponse response = AuthResponse.builder()
                                .token("jwt-token")
                                .user(userResp)
                                .build();

                when(userService.register(any(RegisterRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("jwt-token"));
        }

        @Test
        @DisplayName("POST /api/auth/login: returns token on success")
        void whenLogin_thenReturnsToken() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setEmail("test@example.com");
                request.setPassword("password123");

                UserResponse userResp = UserResponse.builder()
                                .id(1L).email("test@example.com").build();

                AuthResponse response = AuthResponse.builder()
                                .token("jwt-token")
                                .user(userResp)
                                .build();

                when(userService.login(any(LoginRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("jwt-token"));
        }
}
