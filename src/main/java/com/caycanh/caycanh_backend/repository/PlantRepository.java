package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.Plant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PlantRepository extends JpaRepository<Plant, UUID> {

    @Query("SELECT p FROM Plant p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:listingType IS NULL OR p.listingType = :listingType) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Plant> findByFilters(
            @Param("categoryId") UUID categoryId,
            @Param("listingType") Plant.ListingType listingType,
            @Param("status") Plant.PlantStatus status,
            Pageable pageable
    );
}
