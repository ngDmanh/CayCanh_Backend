package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rentals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 1-1 với OrderItem (UNIQUE ở DB)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    // Redundant FK — lưu thêm để query trực tiếp không cần JOIN qua OrderItem
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "total_rental_fee", nullable = false, precision = 14, scale = 0)
    private BigDecimal totalRentalFee;

    // Ngày thực tế trả cây — null nếu chưa trả
    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RentalStatus status = RentalStatus.active;

    // Ghi chú tình trạng cây khi trả
    @Column(name = "condition_on_return", columnDefinition = "TEXT")
    private String conditionOnReturn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Lifecycle ──────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    // ── Enum ───────────────────────────────────────────────────
    public enum RentalStatus { active, returned, overdue }
}
