package com.caycanh.caycanh_backend.dto.rental;

import com.caycanh.caycanh_backend.entity.Plant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull @Min(1) Integer quantity,
        @Min(1) Integer duration,
        Plant.RentDurationUnit durationUnit
) {}
