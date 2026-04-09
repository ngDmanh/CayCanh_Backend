package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(nullable = false)
    private Integer quantity;

    // Snapshot giá tại thời điểm đặt hàng
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal subtotal;

    // ── Quan hệ ────────────────────────────────────────────────
    // 1 OrderItem -> 0 hoặc 1 Rental (chỉ có khi là đơn thuê)
    @OneToOne(mappedBy = "orderItem", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY)
    private Rental rental;
}
