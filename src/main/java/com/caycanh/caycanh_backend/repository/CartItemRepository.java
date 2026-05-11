package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    // Tìm item cụ thể trong giỏ (theo cart + cây + loại)
    // Dùng để check xem cây đã có trong giỏ chưa khi thêm mới
    Optional<CartItem> findByCartIdAndPlantIdAndItemType(
            UUID cartId,
            UUID plantId,
            CartItem.ItemType itemType
    );
}
