package com.caycanh.caycanh_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plant_categories")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlantCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── Quan hệ ────────────────────────────────────────────────
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Plant> plants;
}
