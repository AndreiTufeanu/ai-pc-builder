package com.example.aipcbuilder.repository;

import com.example.aipcbuilder.model.PcComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcComponentRepository extends JpaRepository<PcComponent, Long> {

    List<PcComponent> findAllByOrderByNameAsc();

    List<PcComponent> findByType(PcComponent.ComponentType type);

    @Query("SELECT p FROM PcComponent p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.manufacturer) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<PcComponent> searchByNameOrManufacturer(String searchTerm);

    boolean existsByNameAndModel(String name, String model);
}