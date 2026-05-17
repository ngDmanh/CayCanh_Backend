package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.MessageResponse;
import com.caycanh.caycanh_backend.dto.notification.NotificationResponse;
import com.caycanh.caycanh_backend.dto.notification.UnreadCountResponse;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Lấy danh sách thông báo của user — mới nhất trước.
     * Dùng cho cả khách và admin (mỗi người chỉ thấy của mình).
     */
    @GetMapping("/api/notifications")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(notificationService.getMyNotifications(user, pageable));
    }

    /**
     * Số thông báo chưa đọc — dùng cho badge icon.
     * App gọi định kỳ (mỗi 30 giây hoặc khi user mở app) để cập nhật badge.
     */
    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.getUnreadCount(user)));
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc.
     * App gọi khi user mở màn hình "Thông báo" — badge biến mất.
     */
    @PatchMapping("/api/notifications/mark-all-read")
    public ResponseEntity<MessageResponse> markAllAsRead(
            @AuthenticationPrincipal User user
    ) {
        int count = notificationService.markAllAsRead(user);
        return ResponseEntity.ok(new MessageResponse("Đã đánh dấu " + count + " thông báo là đã đọc"));
    }

    /**
     * Đánh dấu 1 thông báo là đã đọc.
     * App gọi khi user bấm vào thông báo cụ thể trong danh sách.
     */
    @PatchMapping("/api/notifications/{id}/read")
    public ResponseEntity<MessageResponse> markOneAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        notificationService.markOneAsRead(user, id);
        return ResponseEntity.ok(new MessageResponse("Đã đánh dấu là đã đọc"));
    }
}
