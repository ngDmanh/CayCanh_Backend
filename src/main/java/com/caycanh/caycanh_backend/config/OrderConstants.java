package com.caycanh.caycanh_backend.config;

import java.math.BigDecimal;

/**
 * Hằng số nghiệp vụ cho luồng đơn hàng.
 * Tập trung một chỗ để dễ chỉnh khi chính sách thay đổi.
 */
public final class OrderConstants {

    private OrderConstants() {}

    /** Đơn mua vượt mức này phải đặt cọc */
    public static final BigDecimal DEPOSIT_THRESHOLD = new BigDecimal("500000");

    /** Tỷ lệ cọc cho đơn mua lớn: 50% */
    public static final BigDecimal DEPOSIT_RATIO_SALE = new BigDecimal("0.5");

    /** Tỷ lệ cọc cho đơn thuê: 100% (thanh toán đủ trước) */
    public static final BigDecimal DEPOSIT_RATIO_RENT = BigDecimal.ONE;

    /** Số giờ chờ thanh toán trước khi tự hủy đơn */
    public static final int PAYMENT_DEADLINE_HOURS = 24;
}
