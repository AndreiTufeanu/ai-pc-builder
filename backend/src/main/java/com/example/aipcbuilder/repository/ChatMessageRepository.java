package com.example.aipcbuilder.repository;

import com.example.aipcbuilder.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ChatMessage> findByUserIdOrderByCreatedAt(Long userId);

    @Query("SELECT DISTINCT cm.userId FROM ChatMessage cm")
    List<Long> findDistinctUserIds();
}