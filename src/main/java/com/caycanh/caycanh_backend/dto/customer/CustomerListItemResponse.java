package com.caycanh.caycanh_backend.dto.customer;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một dòng trong danh sách khách hàng — không kèm chi tiết đơn.
 * Có thêm 2 số liệu quan trọng: tổng số đơn hoàn thành + số lần bùng hàng.
 */
public record CustomerListItemResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Boolean isActive,
        OffsetDateTime createdAt,
        Long totalCompletedOrders,
        Integer failedDeliveryCount
) {}
