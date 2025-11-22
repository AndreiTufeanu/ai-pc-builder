package com.example.aipcbuilder.repository;

import com.example.aipcbuilder.model.Build;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildRepository extends JpaRepository<Build, Long> {
    List<Build> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserIdAndId(Long userId, Long id);
}