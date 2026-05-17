package com.caycanh.caycanh_backend.dto.customer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Chi tiết 1 khách — bao gồm cả số liệu tổng hợp.
 */
public record CustomerDetailResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String role,
        Boolean isActive,
        OffsetDateTime createdAt,
        // Số liệu tổng hợp
        Long totalOrders,
        Long completedOrders,
        Long cancelledOrders,
        Long failedDeliveries,
        BigDecimal totalSpent,        // tổng tiền đã chi
        Long activeRentals,           // số rental đang thuê
        OffsetDateTime lastOrderAt    // null nếu chưa có đơn
) {}
