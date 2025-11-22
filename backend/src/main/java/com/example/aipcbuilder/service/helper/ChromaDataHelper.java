package com.example.aipcbuilder.service.helper;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ChromaDataHelper {

    public Map<String, Object> createComponentData(PcComponent component) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", component.getId().toString());
        data.put("name", component.getName());
        data.put("type", component.getType().toString());
        data.put("description", component.getDescription());
        data.put("manufacturer", component.getManufacturer());
        data.put("model", component.getModel());
        data.put("price", component.getPrice() != null ? component.getPrice().doubleValue() : null);
        data.put("specifications", component.getSpecifications());
        return data;
    }

    public Map<String, Object> createUserMessageData(ChatMessage message) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", message.getId().toString());
        data.put("user_id", message.getUserId().toString());
        data.put("user_message", message.getUserMessage());
        data.put("ai_response", message.getAiResponse());
        data.put("created_at", message.getCreatedAt().toString());
        return data;
    }

    public <T> HttpEntity<T> createRequest(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}