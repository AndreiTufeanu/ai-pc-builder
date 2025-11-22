package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.LoginRequest;
import com.example.aipcbuilder.dto.LoginResponse;
import com.example.aipcbuilder.dto.SignupRequest;
import com.example.aipcbuilder.dto.SignupResponse;
import com.example.aipcbuilder.model.User;
import com.example.aipcbuilder.repository.UserRepository;
import com.example.aipcbuilder.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthUtils authUtils;

    public AuthController(UserRepository userRepository, AuthUtils authUtils) {
        this.userRepository = userRepository;
        this.authUtils = authUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .map(user -> authUtils.validateLogin(user, request.getPassword()))
                .orElse(ResponseEntity.status(401).body(new LoginResponse(null, null, null, "Invalid username or password")));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return authUtils.badRequest("Username is already taken!");
        }

        if (!authUtils.isValidPassword(request)) {
            return authUtils.badRequest("Password must be at least 6 characters long and match confirmation");
        }

        try {
            User user = authUtils.createUser(request);
            userRepository.save(user);
            return ResponseEntity.ok(new SignupResponse(true, "User registered successfully!"));
        } catch (Exception e) {
            return authUtils.badRequest("Error during registration: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}