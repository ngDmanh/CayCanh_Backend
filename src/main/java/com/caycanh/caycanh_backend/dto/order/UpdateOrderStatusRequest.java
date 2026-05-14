package com.caycanh.caycanh_backend.dto.order;

import com.caycanh.caycanh_backend.entity.Order;
import jakarta.validation.constraints.NotNull;

/**
 * Admin chuyển status: pending → confirmed → completed
 * Hoặc cancelled (từ pending/confirmed).
 */
public record UpdateOrderStatusRequest(
        @NotNull Order.OrderStatus status
) {}
