package com.caycanh.caycanh_backend.dto.rental;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Trả về sau khi tạo rental gia hạn — kèm thông tin đơn cần thanh toán.
 * App khách dùng các field này để hiển thị "đang chờ thanh toán".
 */
public record ExtendRentalResponse(
        UUID newRentalId,
        UUID parentRentalId,
        UUID orderId,                // đơn rental mới được tạo
        BigDecimal totalAmount,
        String message
) {}
