package com.caycanh.caycanh_backend.dto.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Toàn bộ giỏ hàng — items + tổng tiền.
 * Tổng tính tại tầng Service (không lưu DB) để luôn theo giá mới nhất.
 */
public record CartResponse(
        UUID cartId,
        List<CartItemResponse> items,
        Integer totalItems,
        BigDecimal totalAmount
) {}
