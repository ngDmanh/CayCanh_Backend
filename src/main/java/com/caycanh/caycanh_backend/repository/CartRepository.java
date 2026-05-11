package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    // 1 user = 1 cart, dùng để lấy giỏ của user hiện tại
    Optional<Cart> findByUserId(UUID userId);
}
