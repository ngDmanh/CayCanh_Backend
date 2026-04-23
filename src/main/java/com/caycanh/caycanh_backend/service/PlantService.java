package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.plant.*;
import com.caycanh.caycanh_backend.entity.Plant;
import com.caycanh.caycanh_backend.entity.PlantCategory;
import com.caycanh.caycanh_backend.entity.PlantImage;
import com.caycanh.caycanh_backend.repository.PlantCategoryRepository;
import com.caycanh.caycanh_backend.repository.PlantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PlantService {

    private final PlantRepository plantRepository;
    private final PlantCategoryRepository categoryRepository;

    public PlantService(PlantRepository plantRepository, PlantCategoryRepository categoryRepository) {
        this.plantRepository = plantRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<PlantResponse> getAll(UUID categoryId, Plant.ListingType listingType,
                                      Plant.PlantStatus status, Pageable pageable) {
        return plantRepository.findByFilters(categoryId, listingType, status, pageable)
                .map(this::toResponse);
    }

    public PlantResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public PlantResponse create(PlantRequest req) {
        validatePrices(req);
        PlantCategory category = findCategoryOrThrow(req.categoryId());

        Plant plant = Plant.builder()
                .category(category)
                .name(req.name().trim())
                .description(req.description())
                .listingType(req.listingType())
                .priceSale(req.priceSale())
                .priceRentPerMonth(req.priceRentPerMonth())
                .stockQuantity(req.stockQuantity())
                .rentAvailableQty(req.rentAvailableQty())
                .status(req.status() != null ? req.status() : Plant.PlantStatus.active)
                .build();

        if (req.images() != null) {
            List<PlantImage> images = req.images().stream()
                    .map(img -> PlantImage.builder()
                            .plant(plant)
                            .imageUrl(img.imageUrl())
                            .isPrimary(Boolean.TRUE.equals(img.isPrimary()))
                            .sortOrder(img.sortOrder() != null ? img.sortOrder() : 0)
                            .build())
                    .toList();
            plant.setImages(images);
        }

        return toResponse(plantRepository.save(plant));
    }

    @Transactional
    public PlantResponse update(UUID id, PlantRequest req) {
        validatePrices(req);
        Plant plant = findOrThrow(id);
        PlantCategory category = findCategoryOrThrow(req.categoryId());

        plant.setCategory(category);
        plant.setName(req.name().trim());
        plant.setDescription(req.description());
        plant.setListingType(req.listingType());
        plant.setPriceSale(req.priceSale());
        plant.setPriceRentPerMonth(req.priceRentPerMonth());
        plant.setStockQuantity(req.stockQuantity());
        plant.setRentAvailableQty(req.rentAvailableQty());
        if (req.status() != null) plant.setStatus(req.status());

        if (req.images() != null) {
            plant.getImages().clear();
            req.images().forEach(img -> plant.getImages().add(
                    PlantImage.builder()
                            .plant(plant)
                            .imageUrl(img.imageUrl())
                            .isPrimary(Boolean.TRUE.equals(img.isPrimary()))
                            .sortOrder(img.sortOrder() != null ? img.sortOrder() : 0)
                            .build()
            ));
        }

        return toResponse(plantRepository.save(plant));
    }

    @Transactional
    public void delete(UUID id) {
        plantRepository.delete(findOrThrow(id));
    }

    private void validatePrices(PlantRequest req) {
        if (req.listingType() == Plant.ListingType.sale || req.listingType() == Plant.ListingType.both) {
            if (req.priceSale() == null) throw new IllegalArgumentException("priceSale là bắt buộc với loại bán");
        }
        if (req.listingType() == Plant.ListingType.rent || req.listingType() == Plant.ListingType.both) {
            if (req.priceRentPerMonth() == null) throw new IllegalArgumentException("priceRentPerMonth là bắt buộc với loại thuê");
        }
    }

    private Plant findOrThrow(UUID id) {
        return plantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cây cảnh: " + id));
    }

    private PlantCategory findCategoryOrThrow(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục: " + categoryId));
    }

    private PlantResponse toResponse(Plant p) {
        List<PlantImageResponse> images = p.getImages() == null ? List.of() :
                p.getImages().stream()
                        .map(img -> new PlantImageResponse(img.getId(), img.getImageUrl(), img.getIsPrimary(), img.getSortOrder()))
                        .toList();

        return new PlantResponse(
                p.getId(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getName(),
                p.getDescription(),
                p.getListingType().name(),
                p.getPriceSale(),
                p.getPriceRentPerMonth(),
                p.getStockQuantity(),
                p.getRentAvailableQty(),
                p.getStatus().name(),
                p.getCreatedAt(),
                images
        );
    }
}
