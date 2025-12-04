package com.example.aipcbuilder.repository;

import com.example.aipcbuilder.model.PcComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PcComponentRepository extends JpaRepository<PcComponent, Long> {

    List<PcComponent> findAllByOrderByNameAsc();

    @Query("SELECT p.name FROM PcComponent p WHERE p.id = :id")
    Optional<String> findComponentNameById(@Param("id") Long id);
}