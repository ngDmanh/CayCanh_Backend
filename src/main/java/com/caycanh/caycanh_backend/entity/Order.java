package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 0)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.pending;

    @Column(name = "payment_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.unpaid;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "recipient_email", nullable = false, length = 150)
    private String recipientEmail;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "deposit_required", precision = 14, scale = 0, nullable = false)
    @Builder.Default
    private BigDecimal depositRequired = BigDecimal.ZERO;

    @Column(name = "deposit_confirmed_at")
    private OffsetDateTime depositConfirmedAt;

    @Column(name = "deposit_confirmed_by")
    private UUID depositConfirmedBy;

    @Column(name = "payment_deadline")
    private OffsetDateTime paymentDeadline;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Quan hệ ────────────────────────────────────────────────
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Payment> payments;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Review> reviews;

    // ── Lifecycle ──────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Enums ──────────────────────────────────────────────────
    public enum OrderType    { sale, rental }
    public enum OrderStatus {
        pending,
        awaiting_deposit,
        awaiting_payment,
        confirmed,
        delivering,
        completed,
        cancelled,
        delivery_failed
    }
    public enum PaymentStatus { unpaid, paid }
}
