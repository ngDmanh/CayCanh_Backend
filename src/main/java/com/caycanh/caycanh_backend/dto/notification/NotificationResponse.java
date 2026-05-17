package com.caycanh.caycanh_backend.dto.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một thông báo — kèm refType + refId để app điều hướng khi khách bấm.
 * Ví dụ: refType="orders", refId=<orderId> → app mở trang chi tiết đơn hàng.
 */
public record NotificationResponse(
        UUID id,
        String title,
        String body,
        String type,             // order, rental, review, system
        String refType,          // orders | rentals | null
        UUID refId,
        Boolean isRead,
        OffsetDateTime createdAt
) {}
