package com.caycanh.caycanh_backend.dto.plant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PlantResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String name,
        String description,
        String listingType,
        BigDecimal priceSale,
        BigDecimal pricePerDay,
        BigDecimal pricePerWeek,
        BigDecimal pricePerMonth,
        Integer stockQuantity,
        Integer rentAvailableQty,
        String status,
        OffsetDateTime createdAt,
        List<PlantImageResponse> images
) {}