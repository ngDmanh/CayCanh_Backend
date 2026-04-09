package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plants")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private PlantCategory category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // listing_type: 'sale' | 'rent' | 'both'
    // CHECK constraint nằm ở DB — Spring không cần validate lại ở đây
    @Column(name = "listing_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ListingType listingType;

    // Nullable — CHECK constraint ở DB đảm bảo đúng logic
    @Column(name = "price_sale", precision = 12, scale = 0)
    private BigDecimal priceSale;

    @Column(name = "price_rent_per_month", precision = 12, scale = 0)
    private BigDecimal priceRentPerMonth;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "rent_available_qty")
    private Integer rentAvailableQty;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PlantStatus status = PlantStatus.active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Quan hệ ────────────────────────────────────────────────
    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<PlantImage> images;

    @OneToMany(mappedBy = "plant", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "plant", fetch = FetchType.LAZY)
    private List<Rental> rentals;

    @OneToMany(mappedBy = "plant", fetch = FetchType.LAZY)
    private List<Review> reviews;

    // ── Lifecycle ──────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.status == null) this.status = PlantStatus.active;
    }

    // ── Enums ──────────────────────────────────────────────────
    public enum ListingType { sale, rent, both }
    public enum PlantStatus  { active, inactive }
}
