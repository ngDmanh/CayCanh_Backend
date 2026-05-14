package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.cart.*;
import com.caycanh.caycanh_backend.entity.Cart;
import com.caycanh.caycanh_backend.entity.CartItem;
import com.caycanh.caycanh_backend.entity.Plant;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.repository.CartItemRepository;
import com.caycanh.caycanh_backend.repository.CartRepository;
import com.caycanh.caycanh_backend.repository.PlantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PlantRepository plantRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       PlantRepository plantRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.plantRepository = plantRepository;
    }

    // ── Public API ─────────────────────────────────────────────

    @Transactional
    public CartResponse getMyCart(User user) {
        Cart cart = getOrCreateCart(user);
        return toResponse(cart);
    }

    /**
     * Thêm cây vào giỏ. Nếu đã có cây + loại trùng → tăng số lượng.
     * Với rent: cũng cập nhật duration/unit nếu khách đổi.
     */
    @Transactional
    public CartResponse addItem(User user, AddToCartRequest req) {
        Cart cart = getOrCreateCart(user);
        Plant plant = findPlantOrThrow(req.plantId());

        validateAddRequest(req, plant);

        var existing = cartItemRepository.findByCartIdAndPlantIdAndItemType(
                cart.getId(), req.plantId(), req.itemType()
        );

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + req.quantity());
            // Với rent: cho phép đổi duration/unit khi thêm lần nữa
            if (req.itemType() == CartItem.ItemType.rent) {
                if (req.duration() != null) item.setDuration(req.duration());
                if (req.durationUnit() != null) item.setDurationUnit(req.durationUnit());
            }
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .plant(plant)
                    .itemType(req.itemType())
                    .quantity(req.quantity())
                    .duration(req.itemType() == CartItem.ItemType.rent ? req.duration() : null)
                    .durationUnit(req.itemType() == CartItem.ItemType.rent ? req.durationUnit() : null)
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        return toResponse(cart);
    }

    /**
     * Cập nhật số lượng / duration của 1 item.
     */
    @Transactional
    public CartResponse updateItem(User user, UUID itemId, UpdateCartItemRequest req) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findItemInCart(cart, itemId);

        item.setQuantity(req.quantity());
        if (item.getItemType() == CartItem.ItemType.rent) {
            if (req.duration() == null || req.durationUnit() == null) {
                throw new IllegalArgumentException(
                        "Số ngày/tuần/tháng và đơn vị là bắt buộc với cây thuê"
                );
            }
            item.setDuration(req.duration());
            item.setDurationUnit(req.durationUnit());
        }
        cartItemRepository.save(item);

        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(User user, UUID itemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findItemInCart(cart, itemId);
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
        return toResponse(cart);
    }

    // ── Helpers ────────────────────────────────────────────────

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    private Plant findPlantOrThrow(UUID plantId) {
        return plantRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cây: " + plantId));
    }

    private CartItem findItemInCart(Cart cart, UUID itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy item trong giỏ: " + itemId));
    }

    /**
     * Validate logic nghiệp vụ khi thêm cây vào giỏ.
     */
    private void validateAddRequest(AddToCartRequest req, Plant plant) {
        if (plant.getStatus() != Plant.PlantStatus.active) {
            throw new IllegalArgumentException("Cây này hiện không bán/cho thuê");
        }

        if (req.itemType() == CartItem.ItemType.sale) {
            if (plant.getListingType() == Plant.ListingType.rent) {
                throw new IllegalArgumentException("Cây này chỉ cho thuê, không bán");
            }
            if (plant.getStockQuantity() == null || plant.getStockQuantity() < req.quantity()) {
                throw new IllegalArgumentException("Không đủ hàng — chỉ còn "
                        + (plant.getStockQuantity() == null ? 0 : plant.getStockQuantity()));
            }
        } else { // rent
            if (plant.getListingType() == Plant.ListingType.sale) {
                throw new IllegalArgumentException("Cây này chỉ bán, không cho thuê");
            }
            if (plant.getRentAvailableQty() == null || plant.getRentAvailableQty() < req.quantity()) {
                throw new IllegalArgumentException("Không đủ cây cho thuê — chỉ còn "
                        + (plant.getRentAvailableQty() == null ? 0 : plant.getRentAvailableQty()));
            }
            // Thuê bắt buộc có duration và đơn vị
            if (req.duration() == null || req.duration() < 1) {
                throw new IllegalArgumentException("Số ngày/tuần/tháng thuê phải >= 1");
            }
            if (req.durationUnit() == null) {
                throw new IllegalArgumentException("Cần chọn đơn vị thuê: day/week/month");
            }
            // Cây phải có giá tương ứng với đơn vị
            if (plant.getRentPrice(req.durationUnit()) == null) {
                throw new IllegalArgumentException(
                        "Cây này không có giá cho khung " + req.durationUnit()
                );
            }
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQty = items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        return new CartResponse(cart.getId(), items, totalQty, total);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Plant plant = item.getPlant();
        BigDecimal unitPrice;
        BigDecimal subtotal;
        String durationUnitStr = null;

        if (item.getItemType() == CartItem.ItemType.sale) {
            unitPrice = plant.getPriceSale();
            subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        } else {
            // Lấy giá theo đơn vị khách chọn (ngày/tuần/tháng)
            unitPrice = plant.getRentPrice(item.getDurationUnit());
            durationUnitStr = item.getDurationUnit().name();
            // Tổng = giá đơn vị × số duration × số lượng
            subtotal = unitPrice
                    .multiply(BigDecimal.valueOf(item.getDuration()))
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
        }

        String primaryImageUrl = plant.getImages() == null || plant.getImages().isEmpty()
                ? null
                : plant.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .orElse(plant.getImages().get(0))
                .getImageUrl();

        return new CartItemResponse(
                item.getId(),
                plant.getId(),
                plant.getName(),
                primaryImageUrl,
                item.getItemType().name(),
                item.getQuantity(),
                item.getDuration(),
                durationUnitStr,
                unitPrice,
                subtotal
        );
    }
}