package com.caycanh.caycanh_backend.dto.order;

import jakarta.validation.constraints.Size;

/**
 * Admin xác nhận khách đã trả cây thuê.
 * Ghi chú tình trạng cây khi trả (tự do, không bắt buộc).
 */
public record ReturnRentalRequest(
        @Size(max = 1000) String conditionOnReturn
) {}
