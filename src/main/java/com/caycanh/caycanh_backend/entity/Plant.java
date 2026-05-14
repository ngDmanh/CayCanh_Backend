package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cây cảnh — có thể bán, cho thuê, hoặc cả hai.
 *
 * THAY ĐỔI v2:
 * - Bỏ price_rent_per_month
 * - Thêm 3 cột giá: price_per_day, price_per_week, price_per_month
 */
@Entity
@Table(name = "plants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Plant {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private PlantCategory category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType;

    // ── Giá bán ────────────────────────────────────────────────
    @Column(name = "price_sale", precision = 12, scale = 0)
    private BigDecimal priceSale;

    // ── Giá thuê: 3 khung khác nhau ────────────────────────────
    @Column(name = "price_per_day", precision = 12, scale = 0)
    private BigDecimal pricePerDay;

    @Column(name = "price_per_week", precision = 12, scale = 0)
    private BigDecimal pricePerWeek;

    @Column(name = "price_per_month", precision = 12, scale = 0)
    private BigDecimal pricePerMonth;

    // ── Tồn kho ────────────────────────────────────────────────
    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "rent_available_qty")
    private Integer rentAvailableQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PlantStatus status = PlantStatus.active;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PlantImage> images = new ArrayList<>();

    // ── Helper: lấy giá theo khung thời gian ───────────────────
    /**
     * Trả về đơn giá tương ứng với khung thời gian khách chọn.
     * Dùng để tính tiền thuê: giá × số lượng × số duration.
     */
    public BigDecimal getRentPrice(RentDurationUnit unit) {
        return switch (unit) {
            case day   -> pricePerDay;
            case week  -> pricePerWeek;
            case month -> pricePerMonth;
        };
    }

    // ── Enums ──────────────────────────────────────────────────
    public enum ListingType { sale, rent, both }
    public enum PlantStatus { active, inactive, out_of_stock }

    /** Khung thời gian thuê — dùng chung cho cart_items và rentals */
    public enum RentDurationUnit { day, week, month }
}
