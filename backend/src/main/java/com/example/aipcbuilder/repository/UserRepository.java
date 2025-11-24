package com.example.aipcbuilder.repository;

import com.example.aipcbuilder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("SELECT DISTINCT u.id FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN'")
    List<Long> findAdminUserIds();
}