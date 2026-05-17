package com.caycanh.caycanh_backend.dto.stats;

import java.math.BigDecimal;

/**
 * Tổng doanh thu chia theo loại mua/thuê.
 * Dùng cho biểu đồ tròn so sánh tỷ trọng.
 */
public record RevenueByTypeResponse(
        String orderType,
        Long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal avgOrderValue
) {}
