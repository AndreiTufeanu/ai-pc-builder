package com.example.aipcbuilder.utils;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ResponseHelper {

    public ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(createErrorResponse(message));
    }

    public ResponseEntity<?> internalServerError(String message) {
        return ResponseEntity.internalServerError().body(createErrorResponse(message));
    }

    public ResponseEntity<?> unauthorized(String message) {
        return ResponseEntity.status(401).body(createErrorResponse(message));
    }

    public ResponseEntity<?> notFound(String message) {
        return ResponseEntity.status(404).body(createErrorResponse(message));
    }

    public ResponseEntity<?> success(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> successWithData(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    public Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("success", false);
        return response;
    }

    public Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("success", true);
        return response;
    }
}