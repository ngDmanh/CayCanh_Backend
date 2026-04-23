package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.plant.CategoryRequest;
import com.caycanh.caycanh_backend.dto.plant.CategoryResponse;
import com.caycanh.caycanh_backend.entity.PlantCategory;
import com.caycanh.caycanh_backend.repository.PlantCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PlantCategoryService {

    private final PlantCategoryRepository categoryRepository;

    public PlantCategoryService(PlantCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        String slug = resolveSlug(req.slug(), req.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug đã tồn tại: " + slug);
        }
        PlantCategory category = PlantCategory.builder()
                .name(req.name().trim())
                .slug(slug)
                .description(req.description())
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest req) {
        PlantCategory category = findOrThrow(id);
        String slug = resolveSlug(req.slug(), req.name());
        if (!slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug đã tồn tại: " + slug);
        }
        category.setName(req.name().trim());
        category.setSlug(slug);
        category.setDescription(req.description());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        categoryRepository.delete(findOrThrow(id));
    }

    private PlantCategory findOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục: " + id));
    }

    private String resolveSlug(String slug, String name) {
        if (slug != null && !slug.isBlank()) return slug.trim().toLowerCase();
        return name.trim().toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9\\-]", "");
    }

    private CategoryResponse toResponse(PlantCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription());
    }
}
