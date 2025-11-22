package com.example.aipcbuilder.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(Long userId, String userMessage, String aiResponse) {
        this();
        this.userId = userId;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
    }

}