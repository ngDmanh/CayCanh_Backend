package com.caycanh.caycanh_backend.dto.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Đơn hàng đầy đủ thông tin cho khách hoặc admin xem.
 */
public record OrderResponse(
        UUID id,
        String orderType,           // 'sale' hoặc 'rental'
        String status,              // 'pending' | 'confirmed' | 'completed' | 'cancelled'
        String paymentStatus,       // 'unpaid' | 'paid'
        BigDecimal totalAmount,
        String recipientName,
        String recipientPhone,
        String recipientEmail,
        String shippingAddress,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        CustomerInfo customer,      // null nếu khách xem đơn của mình
        List<OrderItemResponse> items
) {
    /** Thông tin khách - chỉ hiển thị khi admin xem */
    public record CustomerInfo(
            UUID id,
            String fullName,
            String email,
            String phone
    ) {}
}
