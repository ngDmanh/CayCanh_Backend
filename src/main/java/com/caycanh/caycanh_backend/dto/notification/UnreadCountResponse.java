package com.caycanh.caycanh_backend.dto.notification;

/**
 * Số thông báo chưa đọc — dùng cho badge icon trên app.
 * Ví dụ: hiển thị "5" trên biểu tượng chuông.
 */
public record UnreadCountResponse(
        long unreadCount
) {}
