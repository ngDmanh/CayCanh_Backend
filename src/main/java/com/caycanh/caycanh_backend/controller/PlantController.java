package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.plant.*;
import com.caycanh.caycanh_backend.entity.Plant;
import com.caycanh.caycanh_backend.service.PlantCategoryService;
import com.caycanh.caycanh_backend.service.PlantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class PlantController {

    private final PlantService plantService;
    private final PlantCategoryService categoryService;

    public PlantController(PlantService plantService, PlantCategoryService categoryService) {
        this.plantService = plantService;
        this.categoryService = categoryService;
    }

    // ── Public: Category ───────────────────────────────────────

    @GetMapping("/api/plants/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/api/plants/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    // ── Public: Plant ──────────────────────────────────────────

    @GetMapping("/api/plants")
    public ResponseEntity<Page<PlantResponse>> getPlants(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Plant.ListingType listingType,
            @RequestParam(required = false, defaultValue = "active") Plant.PlantStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(plantService.getAll(categoryId, listingType, status, pageable));
    }

    @GetMapping("/api/plants/{id}")
    public ResponseEntity<PlantResponse> getPlantById(@PathVariable UUID id) {
        return ResponseEntity.ok(plantService.getById(id));
    }

    // ── Admin: Category ────────────────────────────────────────

    @PostMapping("/api/admin/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(req));
    }

    @PutMapping("/api/admin/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id,
                                                           @Valid @RequestBody CategoryRequest req) {
        return ResponseEntity.ok(categoryService.update(id, req));
    }

    @DeleteMapping("/api/admin/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Admin: Plant ───────────────────────────────────────────

    @PostMapping("/api/admin/plants")
    public ResponseEntity<PlantResponse> createPlant(@Valid @RequestBody PlantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantService.create(req));
    }

    @PutMapping("/api/admin/plants/{id}")
    public ResponseEntity<PlantResponse> updatePlant(@PathVariable UUID id,
                                                     @Valid @RequestBody PlantRequest req) {
        return ResponseEntity.ok(plantService.update(id, req));
    }

    @DeleteMapping("/api/admin/plants/{id}")
    public ResponseEntity<Void> deletePlant(@PathVariable UUID id) {
        plantService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
