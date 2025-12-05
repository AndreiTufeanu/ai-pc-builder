package com.example.aipcbuilder.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponseHelperTest {

    private ResponseHelper responseHelper;

    @BeforeEach
    void setUp() {
        responseHelper = new ResponseHelper();
    }

    @Test
    void testBadRequest() {
        String message = "Invalid input";
        ResponseEntity<?> response = responseHelper.badRequest(message);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(message, body.get("message"));
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testInternalServerError() {
        String message = "Something went wrong";
        ResponseEntity<?> response = responseHelper.internalServerError(message);

        assertNotNull(response);
        assertEquals(500, response.getStatusCodeValue());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(((String) body.get("message")).contains(message));
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testSuccess() {
        String message = "Operation successful";
        ResponseEntity<?> response = responseHelper.success(message);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(message, body.get("message"));
        assertTrue((Boolean) body.get("success"));
    }

    @Test
    void testSuccessWithData() {
        String message = "Data retrieved";
        Object data = Map.of("id", 1, "name", "Test");

        ResponseEntity<?> response = responseHelper.successWithData(message, data);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(message, body.get("message"));
        assertTrue((Boolean) body.get("success"));
        assertEquals(data, body.get("data"));
    }

    @Test
    void testCreateErrorResponse() {
        String message = "Error occurred";
        Map<String, Object> response = responseHelper.createErrorResponse(message);

        assertNotNull(response);
        assertEquals(message, response.get("message"));
        assertFalse((Boolean) response.get("success"));
    }

    @Test
    void testCreateSuccessResponse() {
        String message = "Success!";
        Map<String, Object> response = responseHelper.createSuccessResponse(message);

        assertNotNull(response);
        assertEquals(message, response.get("message"));
        assertTrue((Boolean) response.get("success"));
    }
}