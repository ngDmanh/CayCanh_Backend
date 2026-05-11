package com.caycanh.caycanh_backend.dto.cart;

import com.caycanh.caycanh_backend.entity.CartItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request thêm cây vào giỏ.
 * - durationMonths chỉ bắt buộc khi itemType = rent
 * - validate ở Service vì rule conditional
 */
public record AddToCartRequest(
        @NotNull UUID plantId,
        @NotNull CartItem.ItemType itemType,
        @NotNull @Min(1) Integer quantity,
        @Min(1) Integer durationMonths
) {}
