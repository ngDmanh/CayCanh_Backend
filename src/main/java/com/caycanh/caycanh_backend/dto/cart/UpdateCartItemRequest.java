package com.caycanh.caycanh_backend.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request cập nhật số lượng / số tháng của 1 item trong giỏ.
 * Không cho đổi plantId hay itemType — muốn đổi thì xóa rồi thêm lại.
 */
public record UpdateCartItemRequest(
        @NotNull @Min(1) Integer quantity,
        @Min(1) Integer durationMonths
) {}
