package com.caycanh.caycanh_backend.dto.cart;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một dòng trong giỏ, đã kèm thông tin cây để hiển thị
 * mà không cần app gọi thêm API.
 */
public record CartItemResponse(
        UUID id,
        UUID plantId,
        String plantName,
        String primaryImageUrl,
        String itemType,
        Integer quantity,
        Integer durationMonths,
        BigDecimal unitPrice,    // giá hiện tại (sale hoặc rent/tháng)
        BigDecimal subtotal      // tổng cho dòng này
) {}
