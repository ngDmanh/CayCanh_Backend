package com.caycanh.caycanh_backend.dto.stats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Rental đang active — kèm cảnh báo gần hết hạn.
 * urgency:
 *   - "expired" = đã quá hạn
 *   - "expiring_soon" = còn ≤ 3 ngày
 *   - "normal" = còn nhiều thời gian
 */
public record ActiveRentalResponse(
        UUID rentalId,
        String customerName,
        String customerPhone,
        String plantName,
        LocalDate startDate,
        LocalDate endDate,
        Integer daysRemaining,   // âm = quá hạn
        BigDecimal totalRentalFee,
        String status,
        String urgency
) {}
