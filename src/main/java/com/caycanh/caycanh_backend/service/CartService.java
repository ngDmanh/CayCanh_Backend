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

    /**
     * Lấy giỏ của user hiện tại — tự tạo nếu chưa có.
     */
    @Transactional
    public CartResponse getMyCart(User user) {
        Cart cart = getOrCreateCart(user);
        return toResponse(cart);
    }

    /**
     * Thêm cây vào giỏ. Nếu đã có cây + loại trùng → tăng số lượng,
     * không tạo dòng mới (nhờ UNIQUE constraint ở DB).
     */
    @Transactional
    public CartResponse addItem(User user, AddToCartRequest req) {
        Cart cart = getOrCreateCart(user);
        Plant plant = findPlantOrThrow(req.plantId());

        // Validate input theo nghiệp vụ
        validateAddRequest(req, plant);

        // Nếu đã có item trùng (cùng plant + itemType) → cộng quantity
        var existing = cartItemRepository.findByCartIdAndPlantIdAndItemType(
                cart.getId(), req.plantId(), req.itemType()
        );

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + req.quantity());
            // Cho phép đổi durationMonths nếu user thêm lần nữa với số tháng khác
            if (req.itemType() == CartItem.ItemType.rent && req.durationMonths() != null) {
                item.setDurationMonths(req.durationMonths());
            }
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .plant(plant)
                    .itemType(req.itemType())
                    .quantity(req.quantity())
                    .durationMonths(req.itemType() == CartItem.ItemType.rent
                            ? req.durationMonths() : null)
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        // Trigger DB tự cập nhật updated_at của cart
        return toResponse(cart);
    }

    /**
     * Cập nhật số lượng / số tháng của 1 item.
     */
    @Transactional
    public CartResponse updateItem(User user, UUID itemId, UpdateCartItemRequest req) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findItemInCart(cart, itemId);

        item.setQuantity(req.quantity());
        if (item.getItemType() == CartItem.ItemType.rent) {
            if (req.durationMonths() == null) {
                throw new IllegalArgumentException("Số tháng thuê là bắt buộc với cây thuê");
            }
            item.setDurationMonths(req.durationMonths());
        }
        cartItemRepository.save(item);

        return toResponse(cart);
    }

    /**
     * Xóa 1 item khỏi giỏ.
     */
    @Transactional
    public CartResponse removeItem(User user, UUID itemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findItemInCart(cart, itemId);
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return toResponse(cart);
    }

    /**
     * Xóa toàn bộ giỏ — dùng khi đặt hàng xong hoặc user muốn clear.
     */
    @Transactional
    public CartResponse clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
        return toResponse(cart);
    }

    // ── Helpers ────────────────────────────────────────────────

    /**
     * Tìm hoặc tạo giỏ cho user. Mỗi user chỉ có 1 giỏ (UNIQUE ở DB).
     */
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
        // Cây phải đang active
        if (plant.getStatus() != Plant.PlantStatus.active) {
            throw new IllegalArgumentException("Cây này hiện không bán/cho thuê");
        }

        // Loại item phải khớp với listing_type của cây
        if (req.itemType() == CartItem.ItemType.sale) {
            if (plant.getListingType() == Plant.ListingType.rent) {
                throw new IllegalArgumentException("Cây này chỉ cho thuê, không bán");
            }
            // Kiểm tra tồn kho
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
            // Thuê bắt buộc có số tháng
            if (req.durationMonths() == null || req.durationMonths() < 1) {
                throw new IllegalArgumentException("Số tháng thuê phải >= 1");
            }
        }
    }

    /**
     * Convert Cart entity → CartResponse, tự tính giá theo giá HIỆN TẠI của cây.
     * Quan trọng: không lưu giá trong cart_items — luôn lấy từ plants để khách
     * thấy đúng giá hiện tại nếu admin vừa cập nhật.
     */
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

        if (item.getItemType() == CartItem.ItemType.sale) {
            unitPrice = plant.getPriceSale();
            subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        } else {
            unitPrice = plant.getPriceRentPerMonth();
            // Thuê: giá/tháng × số tháng × số lượng
            subtotal = unitPrice
                    .multiply(BigDecimal.valueOf(item.getDurationMonths()))
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
        }

        // Lấy ảnh đại diện (is_primary = true), fallback ảnh đầu tiên
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
                item.getDurationMonths(),
                unitPrice,
                subtotal
        );
    }
}
