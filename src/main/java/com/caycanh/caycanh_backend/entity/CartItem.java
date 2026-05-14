package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Một dòng trong giỏ hàng.
 *
 * THAY ĐỔI v2:
 * - Bỏ duration_months
 * - Thêm duration + duration_unit (day/week/month)
 */
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "plant_id", "item_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 10)
    private ItemType itemType;

    @Column(nullable = false)
    private Integer quantity;

    // ── Thông tin thuê (chỉ áp dụng khi itemType = rent) ───────
    /** Số ngày/tuần/tháng khách muốn thuê — null nếu là sale */
    @Column
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", length = 10)
    private Plant.RentDurationUnit durationUnit;

    public enum ItemType { sale, rent }
}
