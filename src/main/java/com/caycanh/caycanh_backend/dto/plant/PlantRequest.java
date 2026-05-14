package com.caycanh.caycanh_backend.dto.plant;

import com.caycanh.caycanh_backend.entity.Plant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlantRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull Plant.ListingType listingType,
        BigDecimal priceSale,
        BigDecimal pricePerDay,
        BigDecimal pricePerWeek,
        BigDecimal pricePerMonth,
        Integer stockQuantity,
        Integer rentAvailableQty,
        Plant.PlantStatus status,
        @Valid List<PlantImageRequest> images
) {}