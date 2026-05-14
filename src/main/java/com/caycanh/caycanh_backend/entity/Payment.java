package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Mã giao dịch từ cổng thanh toán — null khi COD
    @Column(name = "transaction_id", unique = true, length = 200)
    private String transactionId;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amount;

    // Phương thức thanh toán: cash (COD) hoặc bank_transfer (chuyển khoản)
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentMethod method = PaymentMethod.cash;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.pending;

    // Null nếu chưa thu tiền, có giá trị khi đã thu
    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    // ── Enums ──────────────────────────────────────────────────
    public enum PaymentMethod { cash, bank_transfer }
    public enum PaymentStatus { pending, success, failed, refunded }
}
