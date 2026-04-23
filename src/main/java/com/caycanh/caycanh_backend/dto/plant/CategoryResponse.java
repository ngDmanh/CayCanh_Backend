package com.caycanh.caycanh_backend.dto.plant;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description
) {}
