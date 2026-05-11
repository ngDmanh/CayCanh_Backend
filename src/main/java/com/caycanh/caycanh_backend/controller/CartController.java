package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.cart.AddToCartRequest;
import com.caycanh.caycanh_backend.dto.cart.CartResponse;
import com.caycanh.caycanh_backend.dto.cart.UpdateCartItemRequest;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Tất cả endpoint dưới /api/cart đều cần đăng nhập (đã được SecurityConfig
 * bảo vệ qua anyRequest().authenticated()).
 *
 * @AuthenticationPrincipal tự inject User từ SecurityContext — đây chính là
 * User object mà JwtFilter đã set khi xác thực token.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** Xem giỏ hàng của user hiện tại. Tự tạo nếu chưa có. */
    @GetMapping
    public ResponseEntity<CartResponse> getMyCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cartService.getMyCart(user));
    }

    /** Thêm cây vào giỏ. */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddToCartRequest req
    ) {
        return ResponseEntity.ok(cartService.addItem(user, req));
    }

    /** Cập nhật số lượng / số tháng của 1 item trong giỏ. */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal User user,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest req
    ) {
        return ResponseEntity.ok(cartService.updateItem(user, itemId, req));
    }

    /** Xóa 1 item khỏi giỏ. */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(cartService.removeItem(user, itemId));
    }

    /** Xóa toàn bộ giỏ. */
    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cartService.clearCart(user));
    }
}
