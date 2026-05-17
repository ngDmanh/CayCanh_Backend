package com.caycanh.caycanh_backend.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Khách tạo đánh giá cho 1 cây trong 1 đơn cụ thể.
 * - orderId bắt buộc: chứng minh khách thực sự đã mua/thuê cây này
 * - rating 1-5 sao
 * - comment tùy chọn
 */
public record CreateReviewRequest(
        @NotNull UUID plantId,
        @NotNull UUID orderId,
        @NotNull @Min(1) @Max(5) Short rating,
        @Size(max = 2000) String comment
) {}
