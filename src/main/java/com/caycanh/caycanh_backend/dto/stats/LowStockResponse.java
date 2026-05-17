package com.caycanh.caycanh_backend.dto.stats;

import java.util.UUID;

/**
 * Cây sắp hết hàng — cảnh báo admin nhập thêm.
 */
public record LowStockResponse(
        UUID id,
        String name,
        String listingType,
        Integer stockQuantity,
        Integer rentAvailableQty,
        String categoryName
) {}
