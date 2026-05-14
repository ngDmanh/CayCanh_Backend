package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.order.*;
import com.caycanh.caycanh_backend.entity.Order;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── Customer endpoints ─────────────────────────────────────

    @PostMapping("/api/orders/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CheckoutRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.checkout(user, req));
    }

    @GetMapping("/api/orders/my")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.getMyOrders(user, status, pageable));
    }

    @GetMapping("/api/orders/my/{id}")
    public ResponseEntity<OrderResponse> getMyOrderById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(orderService.getMyOrderById(user, id));
    }

    /** Khách tự hủy đơn (chỉ khi chưa giao) */
    @PatchMapping("/api/orders/my/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelMyOrder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(orderService.cancelMyOrder(user, id));
    }

    // ── Admin endpoints ────────────────────────────────────────

    @GetMapping("/api/admin/orders")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(required = false) Order.OrderType orderType,
            @RequestParam(required = false) Order.PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                orderService.getAllOrders(status, orderType, paymentStatus, pageable));
    }

    @GetMapping("/api/admin/orders/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /** Xác nhận đơn nhỏ COD: pending → confirmed */
    @PatchMapping("/api/admin/orders/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    /** Xác nhận đã nhận cọc/thanh toán: awaiting_* → confirmed */
    @PatchMapping("/api/admin/orders/{id}/confirm-deposit")
    public ResponseEntity<OrderResponse> confirmDeposit(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin
    ) {
        return ResponseEntity.ok(orderService.confirmDeposit(id, admin.getId()));
    }

    /** Bắt đầu giao: confirmed → delivering */
    @PatchMapping("/api/admin/orders/{id}/start-delivery")
    public ResponseEntity<OrderResponse> startDelivery(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.startDelivery(id));
    }

    /** Xác nhận đã thu tiền (COD hoặc thu nốt) */
    @PatchMapping("/api/admin/orders/{id}/payment")
    public ResponseEntity<OrderResponse> markAsPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.markAsPaid(id));
    }

    /** Hoàn thành đơn: delivering → completed (bắt buộc đã paid) */
    @PatchMapping("/api/admin/orders/{id}/complete")
    public ResponseEntity<OrderResponse> completeOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.completeOrder(id));
    }

    /** Đánh dấu giao thất bại (bùng hàng) */
    @PatchMapping("/api/admin/orders/{id}/delivery-failed")
    public ResponseEntity<OrderResponse> markDeliveryFailed(
            @PathVariable UUID id,
            @Valid @RequestBody DeliveryFailedRequest req
    ) {
        return ResponseEntity.ok(orderService.markDeliveryFailed(id, req.reason()));
    }
}
