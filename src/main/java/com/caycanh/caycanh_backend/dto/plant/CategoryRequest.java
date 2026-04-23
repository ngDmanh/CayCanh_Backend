package com.caycanh.caycanh_backend.dto.plant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 120) String slug,
        String description
) {}
