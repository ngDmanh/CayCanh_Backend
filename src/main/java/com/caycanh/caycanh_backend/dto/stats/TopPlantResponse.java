package com.caycanh.caycanh_backend.dto.stats;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cây bán/cho thuê tốt nhất — sắp xếp theo doanh thu.
 */
public record TopPlantResponse(
        UUID id,
        String name,
        String listingType,
        Long totalQuantity,      // tổng số lượng đã bán/thuê
        BigDecimal totalRevenue,
        BigDecimal avgRating,    // null nếu chưa có review
        Long reviewCount
) {}
