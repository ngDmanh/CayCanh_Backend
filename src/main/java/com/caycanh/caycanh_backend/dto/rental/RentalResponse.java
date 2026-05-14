package com.caycanh.caycanh_backend.dto.rental;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Rental — dùng cho cả customer xem và admin xem.
 */
public record RentalResponse(
        UUID id,
        UUID plantId,
        String plantName,
        String primaryImageUrl,
        Integer duration,
        String durationUnit,
        String startDate,           // null nếu pending_delivery
        String endDate,             // null nếu pending_delivery
        String status,
        BigDecimal totalRentalFee,
        String actualReturnDate,
        String conditionOnReturn,
        UUID parentRentalId,        // null nếu là rental gốc (không phải gia hạn)
        OffsetDateTime createdAt,
        CustomerInfo customer       // null nếu khách tự xem
) {
    public record CustomerInfo(
            UUID id,
            String fullName,
            String email,
            String phone
    ) {}
}
