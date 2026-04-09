package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_cart_plant_type",
        columnNames = {"cart_id", "plant_id", "item_type"}
    )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    // 'sale' hoặc 'rent' — người dùng chọn khi thêm vào giỏ
    @Column(name = "item_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // Chỉ có giá trị khi itemType = rent
    // CHECK constraint ở DB đảm bảo NOT NULL khi rent
    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "added_at", nullable = false, updatable = false)
    private OffsetDateTime addedAt;

    // ── Lifecycle ──────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.addedAt = OffsetDateTime.now();
    }

    // ── Enum ───────────────────────────────────────────────────
    public enum ItemType { sale, rent }
}
