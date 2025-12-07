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

    @Query("SELECT p FROM PcComponent p ORDER BY " +
            "CASE p.type " +
            "  WHEN 'CPU' THEN 1 " +
            "  WHEN 'GPU' THEN 2 " +
            "  WHEN 'PSU' THEN 3 " +
            "  WHEN 'RAM' THEN 4 " +
            "  WHEN 'STORAGE' THEN 5 " +
            "  WHEN 'MOTHERBOARD' THEN 6 " +
            "  WHEN 'CASE' THEN 7 " +
            "END, " +
            "p.manufacturer ASC, " +
            "p.id ASC")
    List<PcComponent> findAllByOrderByTypeManufacturerId();

    @Query("SELECT p.name FROM PcComponent p WHERE p.id = :id")
    Optional<String> findComponentNameById(@Param("id") Long id);
}