package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /** Tìm khách hàng (role = customer) + filter theo tên/email/sđt */
    @Query("SELECT u FROM User u WHERE u.role = 'customer' AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " u.phone LIKE CONCAT('%', :search, '%')) " +
            "ORDER BY u.createdAt DESC")
    Page<User> searchCustomers(@Param("search") String search, Pageable pageable);
}