package com.caycanh.caycanh_backend.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một dòng trong đơn. unitPrice là snapshot tại thời điểm đặt - 
 * dù admin có đổi giá sau này, đơn cũ vẫn hiển thị đúng giá khách đã mua.
 */
public record OrderItemResponse(
        UUID id,
        UUID plantId,
        String plantName,
        String primaryImageUrl,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        RentalInfo rental    // null nếu là đơn sale
) {
    /** Thông tin thuê - chỉ có với đơn rental */
    public record RentalInfo(
            UUID rentalId,
            String startDate,
            String endDate,
            Integer duration,
            String status,
            String actualReturnDate
    ) {}
}
