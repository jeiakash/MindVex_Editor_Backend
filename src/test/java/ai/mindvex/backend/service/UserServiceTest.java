package ai.mindvex.backend.service;

import ai.mindvex.backend.dto.AuthResponse;
import ai.mindvex.backend.dto.LoginRequest;
import ai.mindvex.backend.dto.RegisterRequest;
import ai.mindvex.backend.dto.UserResponse;
import ai.mindvex.backend.entity.User;
import ai.mindvex.backend.exception.ResourceNotFoundException;
import ai.mindvex.backend.repository.UserRepository;
import ai.mindvex.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encoded_password")
                .fullName("Test User")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========== REGISTER ==========

    @Test
    @DisplayName("register: when valid request, then returns auth response with tokens")
    void whenValidRegisterRequest_thenReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt_token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh_token");

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertNotNull(response.getUser());
        assertEquals("test@example.com", response.getUser().getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: when email already registered, then throws IllegalArgumentException")
    void whenEmailAlreadyExists_thenThrowIllegalArgument() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));
        verify(userRepository, never()).save(any());
    }

    // ========== LOGIN ==========

    @Test
    @DisplayName("login: when valid credentials, then returns auth response with tokens")
    void whenValidLogin_thenReturnAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // authenticate returns void-like on success
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt_token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh_token");

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("refresh_token", response.getRefreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // ========== GET CURRENT USER ==========

    @Test
    @DisplayName("getCurrentUser: when user exists, then returns user response")
    void whenUserExists_thenReturnUserResponse() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getCurrentUser("test@example.com");

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
    }

    @Test
    @DisplayName("getCurrentUser: when user not found, then throws ResourceNotFoundException")
    void whenUserNotFound_thenThrowException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getCurrentUser("unknown@example.com"));
    }
}
