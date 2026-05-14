package com.caycanh.caycanh_backend.dto.rental;

import jakarta.validation.constraints.Size;

/**
 * Admin đánh dấu đã thu hồi cây.
 * Ghi chú tình trạng tự do, không bắt buộc.
 */
public record CollectRentalRequest(
        @Size(max = 1000) String conditionOnReturn
) {}
