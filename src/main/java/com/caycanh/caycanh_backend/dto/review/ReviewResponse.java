package com.caycanh.caycanh_backend.dto.review;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một đánh giá — kèm tên người đánh giá để hiển thị công khai.
 */
public record ReviewResponse(
        UUID id,
        UUID plantId,
        String plantName,
        UUID orderId,
        UUID userId,
        String userName,        // tên người đánh giá
        Short rating,
        String comment,
        OffsetDateTime createdAt
) {}
