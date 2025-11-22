package com.example.aipcbuilder.utils;

import com.example.aipcbuilder.dto.LoginRequest;
import com.example.aipcbuilder.dto.LoginResponse;
import com.example.aipcbuilder.dto.SignupRequest;
import com.example.aipcbuilder.dto.SignupResponse;
import com.example.aipcbuilder.model.Role;
import com.example.aipcbuilder.model.User;
import com.example.aipcbuilder.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class AuthUtils {

    private final RoleRepository roleRepository;

    public AuthUtils(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public ResponseEntity<LoginResponse> validateLogin(User user, String password) {
        if (!user.getPassword().equals(password)) {
            return ResponseEntity.status(401).body(new LoginResponse(null, null, null, "Invalid username or password"));
        }
        if (!user.isEnabled()) {
            return ResponseEntity.status(401).body(new LoginResponse(null, null, null, "Account is disabled"));
        }

        String[] roles = user.getRoles().stream()
                .map(Role::getName)
                .toArray(String[]::new);

        return ResponseEntity.ok(new LoginResponse(user.getId(), user.getUsername(), roles, "Login successful"));
    }

    public boolean isValidPassword(SignupRequest request) {
        return request.getPassword() != null &&
                request.getPassword().length() >= 6 &&
                request.getPassword().equals(request.getConfirmPassword());
    }

    public User createUser(SignupRequest request) {
        User user = new User(request.getUsername(), request.getPassword());
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_USER not found."));
        user.setRoles(Collections.singleton(userRole));
        return user;
    }

    public ResponseEntity<SignupResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new SignupResponse(false, message));
    }
}