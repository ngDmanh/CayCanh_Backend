package com.caycanh.caycanh_backend.dto.stats;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tổng quan đơn hàng — view này dùng để hiển thị bảng đơn nhanh
 * (không cần load OrderItem chi tiết).
 */
public record OrderSummaryResponse(
        UUID id,
        String orderType,
        String status,
        String paymentStatus,
        BigDecimal totalAmount,
        OffsetDateTime createdAt,
        String customerName,
        String customerPhone,
        Long itemCount
) {}
