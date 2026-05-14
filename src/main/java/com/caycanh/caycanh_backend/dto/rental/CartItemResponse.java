package com.caycanh.caycanh_backend.dto.rental;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một dòng trong giỏ — kèm thông tin cây và đơn giá theo khung.
 */
public record CartItemResponse(
        UUID id,
        UUID plantId,
        String plantName,
        String primaryImageUrl,
        String itemType,
        Integer quantity,
        Integer duration,
        String durationUnit,
        BigDecimal unitPrice,    // giá/ngày, giá/tuần hoặc giá/tháng tùy unit
        BigDecimal subtotal      // tổng dòng = unitPrice × duration × quantity
) {}
