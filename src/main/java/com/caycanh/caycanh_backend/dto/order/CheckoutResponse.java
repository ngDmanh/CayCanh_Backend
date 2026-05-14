package com.caycanh.caycanh_backend.dto.order;

import java.util.List;

/**
 * Một lần checkout có thể tạo ra 1 hoặc 2 đơn:
 * - Chỉ mua → 1 đơn sale
 * - Chỉ thuê → 1 đơn rental
 * - Cả hai → 2 đơn (mỗi loại 1)
 */
public record CheckoutResponse(
        List<OrderResponse> orders,
        String message
) {}
