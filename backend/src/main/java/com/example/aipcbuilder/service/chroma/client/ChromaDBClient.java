package com.example.aipcbuilder.service.chroma.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ChromaDBClient {

    @Value("${chromadb.server.url:http://localhost:8000}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public <T> T post(String endpoint, Object request, Class<T> responseType) {
        HttpEntity<?> entity = createJsonRequest(request);
        ResponseEntity<T> response = restTemplate.postForEntity(
                baseUrl + endpoint,
                entity,
                responseType
        );
        return response.getBody();
    }

    public <T> T post(String endpoint, Object request, ParameterizedTypeReference<T> responseType) {
        HttpEntity<?> entity = createJsonRequest(request);
        ResponseEntity<T> response = restTemplate.exchange(
                baseUrl + endpoint,
                HttpMethod.POST,
                entity,
                responseType
        );
        return response.getBody();
    }

    public void delete(String endpoint) {
        restTemplate.delete(baseUrl + endpoint);
    }

    public String get(String endpoint) {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + endpoint, String.class);
        return response.getBody();
    }

    public String getStatus() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK ? "CONNECTED" : "ERROR";
        } catch (Exception e) {
            return "DISCONNECTED";
        }
    }

    private HttpEntity<?> createJsonRequest(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(body, headers);
    }
}