package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Lấy danh sách thông báo của user, mới nhất trước */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /** Đếm số thông báo chưa đọc — dùng cho badge */
    long countByUserIdAndIsReadFalse(UUID userId);

    /**
     * Đánh dấu TẤT CẢ thông báo chưa đọc của user thành đã đọc.
     * Gọi khi khách mở màn hình "Thông báo".
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true " +
           "WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId);
}
