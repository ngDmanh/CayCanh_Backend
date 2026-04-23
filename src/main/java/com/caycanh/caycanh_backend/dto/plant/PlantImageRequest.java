package com.caycanh.caycanh_backend.dto.plant;

import jakarta.validation.constraints.NotBlank;

public record PlantImageRequest(
        @NotBlank String imageUrl,
        Boolean isPrimary,
        Integer sortOrder
) {}
