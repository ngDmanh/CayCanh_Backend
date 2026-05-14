package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Hợp đồng thuê cây — mỗi cây thuê là 1 rental record.
 *
 * THAY ĐỔI v2:
 * - start_date, end_date có thể null (khi pending_delivery)
 * - Bỏ duration_months → duration + duration_unit
 * - Status mới: pending_delivery
 * - Thêm parent_rental_id để liên kết chuỗi gia hạn
 */
@Entity
@Table(name = "rentals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rental {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    // ── Thời gian — null cho đến khi admin giao cây ────────────
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // ── Thời hạn thuê: số + đơn vị ─────────────────────────────
    @Column(nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false, length = 10)
    private Plant.RentDurationUnit durationUnit;

    @Column(name = "total_rental_fee", precision = 14, scale = 0, nullable = false)
    private BigDecimal totalRentalFee;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    /** Ghi chú text về tình trạng cây khi thu hồi (tùy chọn) */
    @Column(name = "condition_on_return", columnDefinition = "TEXT")
    private String conditionOnReturn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RentalStatus status = RentalStatus.pending_delivery;

    /** Link đến rental cũ nếu đây là gia hạn — null nếu là rental gốc */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_rental_id")
    private Rental parentRental;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Helper: tính end_date từ start + duration ──────────────
    public LocalDate computeEndDate(LocalDate startDate) {
        return switch (durationUnit) {
            case day   -> startDate.plusDays(duration);
            case week  -> startDate.plusWeeks(duration);
            case month -> startDate.plusMonths(duration);
        };
    }

    public enum RentalStatus { pending_delivery, active, returned, overdue }
}
