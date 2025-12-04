package com.example.aipcbuilder.service.auth;

import com.example.aipcbuilder.dto.LoginRequest;
import com.example.aipcbuilder.dto.LoginResponse;
import com.example.aipcbuilder.dto.SignupRequest;
import com.example.aipcbuilder.dto.SignupResponse;
import com.example.aipcbuilder.model.Role;
import com.example.aipcbuilder.model.User;
import com.example.aipcbuilder.repository.RoleRepository;
import com.example.aipcbuilder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public LoginResponse login(LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .map(user -> validateLogin(user, request.getPassword()))
                .orElse(new LoginResponse(null, null, null, "Invalid username or password"));
    }

    private LoginResponse validateLogin(User user, String password) {
        if (!user.getPassword().equals(password)) {
            return new LoginResponse(null, null, null, "Invalid username or password");
        }
        if (!user.isEnabled()) {
            return new LoginResponse(null, null, null, "Account is disabled");
        }

        String[] roles = user.getRoles().stream()
                .map(Role::getName)
                .toArray(String[]::new);

        return new LoginResponse(user.getId(), user.getUsername(), roles, "Login successful");
    }

    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return new SignupResponse(false, "Username is already taken!");
        }

        try {
            User user = createUser(request);
            userRepository.save(user);
            return new SignupResponse(true, "User registered successfully!");
        } catch (Exception e) {
            return new SignupResponse(false, "Error during registration: " + e.getMessage());
        }
    }

    private User createUser(SignupRequest request) {
        User user = new User(request.getUsername(), request.getPassword());
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_USER not found."));
        user.setRoles(Collections.singleton(userRole));
        return user;
    }
}