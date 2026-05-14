package com.caycanh.caycanh_backend.dto.rental;

import com.caycanh.caycanh_backend.entity.Plant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Khách bấm gia hạn — chọn khung thời gian mới.
 * Sẽ tạo Rental mới link về parent_rental_id, tính tiền theo giá hiện tại.
 */
public record ExtendRentalRequest(
        @NotNull @Min(1) Integer duration,
        @NotNull Plant.RentDurationUnit durationUnit
) {}
