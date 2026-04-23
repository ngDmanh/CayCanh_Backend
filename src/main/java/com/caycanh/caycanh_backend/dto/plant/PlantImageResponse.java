package com.caycanh.caycanh_backend.dto.plant;

import java.util.UUID;

public record PlantImageResponse(
        UUID id,
        String imageUrl,
        Boolean isPrimary,
        Integer sortOrder
) {}
