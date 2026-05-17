package com.caycanh.caycanh_backend.dto.stats;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Doanh thu theo tháng × theo loại đơn.
 * Mỗi tháng có thể có 2 dòng (1 cho sale, 1 cho rental).
 */
public record RevenueMonthlyResponse(
        OffsetDateTime month,
        String orderType,        // "sale" | "rental"
        Long totalOrders,
        BigDecimal revenue
) {}
