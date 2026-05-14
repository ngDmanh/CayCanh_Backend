package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId " +
            "AND (:status IS NULL OR o.status = :status) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByUserId(
            @Param("userId") UUID userId,
            @Param("status") Order.OrderStatus status,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:orderType IS NULL OR o.orderType = :orderType) AND " +
            "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)")
    Page<Order> findByFilters(
            @Param("status") Order.OrderStatus status,
            @Param("orderType") Order.OrderType orderType,
            @Param("paymentStatus") Order.PaymentStatus paymentStatus,
            Pageable pageable
    );

    /**
     * Tìm đơn quá hạn thanh toán để auto-cancel.
     * Đơn ở awaiting_deposit/awaiting_payment + đã quá payment_deadline.
     */
    @Query("SELECT o FROM Order o WHERE " +
            "o.status IN ('awaiting_deposit', 'awaiting_payment') AND " +
            "o.paymentDeadline < :now")
    List<Order> findExpiredAwaitingPayment(@Param("now") OffsetDateTime now);
}
