package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.LoginRequest;
import com.example.aipcbuilder.dto.LoginResponse;
import com.example.aipcbuilder.dto.SignupRequest;
import com.example.aipcbuilder.dto.SignupResponse;
import com.example.aipcbuilder.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        if (response.getUserId() == null) {
            return ResponseEntity.status(401).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}