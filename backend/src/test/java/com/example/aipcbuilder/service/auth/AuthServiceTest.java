package com.example.aipcbuilder.service.auth;

import com.example.aipcbuilder.dto.LoginRequest;
import com.example.aipcbuilder.dto.LoginResponse;
import com.example.aipcbuilder.dto.SignupRequest;
import com.example.aipcbuilder.dto.SignupResponse;
import com.example.aipcbuilder.model.Role;
import com.example.aipcbuilder.model.User;
import com.example.aipcbuilder.repository.RoleRepository;
import com.example.aipcbuilder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(userRole));
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertArrayEquals(new String[]{"ROLE_USER"}, response.getRoles());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void testLogin_UserNotFound() {
        LoginRequest request = new LoginRequest("nonexistent", "password");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNull(response.getUserId());
        assertEquals("Invalid username or password", response.getMessage());
    }

    @Test
    void testLogin_WrongPassword() {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNull(response.getUserId());
        assertEquals("Invalid username or password", response.getMessage());
    }

    @Test
    void testSignup_Success() {
        SignupRequest request = new SignupRequest("newuser", "password123", "password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        SignupResponse response = authService.signup(request);

        assertTrue(response.isSuccess());
        assertEquals("User registered successfully!", response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testSignup_UsernameTaken() {
        SignupRequest request = new SignupRequest("existinguser", "password123", "password123");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        SignupResponse response = authService.signup(request);

        assertFalse(response.isSuccess());
        assertEquals("Username is already taken!", response.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}