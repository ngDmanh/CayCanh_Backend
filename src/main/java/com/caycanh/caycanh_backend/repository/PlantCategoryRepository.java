package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.PlantCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlantCategoryRepository extends JpaRepository<PlantCategory, UUID> {
    boolean existsBySlug(String slug);
    Optional<PlantCategory> findBySlug(String slug);
}
