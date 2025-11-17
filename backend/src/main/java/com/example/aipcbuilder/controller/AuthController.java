package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.LoginRequest;
import com.example.aipcbuilder.dto.LoginResponse;
import com.example.aipcbuilder.dto.SignupRequest;
import com.example.aipcbuilder.dto.SignupResponse;
import com.example.aipcbuilder.model.Role;
import com.example.aipcbuilder.model.User;
import com.example.aipcbuilder.repository.RoleRepository;
import com.example.aipcbuilder.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AuthController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // Find user by username
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(401).body(new LoginResponse(null, null, null, "Invalid username or password"));
        }

        User user = userOptional.get();

        // Check password
        if (!user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401).body(new LoginResponse(null, null, null, "Invalid username or password"));
        }

        // Check if user is enabled
        if (!user.isEnabled()) {
            return ResponseEntity.status(401).body(new LoginResponse(null, null, null, "Account is disabled"));
        }

        // Convert roles to array
        String[] roles = user.getRoles().stream()
                .map(role -> role.getName())
                .toArray(String[]::new);

        // Return user ID in the response
        LoginResponse response = new LoginResponse(
                user.getId(), // Add user ID
                user.getUsername(),
                roles,
                "Login successful"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new SignupResponse(false, "Username is already taken!"));
        }

        // Validate password
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(new SignupResponse(false, "Password must be at least 6 characters long"));
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(new SignupResponse(false, "Passwords do not match"));
        }

        try {
            // Create new user
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setEnabled(true);

            // Assign ROLE_USER by default
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Error: Role ROLE_USER not found."));
            user.setRoles(Collections.singleton(userRole));

            userRepository.save(user);

            return ResponseEntity.ok(new SignupResponse(true, "User registered successfully!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new SignupResponse(false, "Error during registration: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}