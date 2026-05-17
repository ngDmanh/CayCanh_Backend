package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /** Xem đã có review cho bộ (user + plant + order) chưa — chặn review trùng */
    boolean existsByUserIdAndPlantIdAndOrderId(UUID userId, UUID plantId, UUID orderId);

    /** Review của 1 cây — hiển thị công khai trên trang cây */
    @Query("SELECT r FROM Review r WHERE r.plant.id = :plantId ORDER BY r.createdAt DESC")
    Page<Review> findByPlantId(@Param("plantId") UUID plantId, Pageable pageable);

    /** Review của 1 user — xem lịch sử đánh giá của mình */
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    Page<Review> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /** Điểm trung bình của 1 cây — dùng cho trang chi tiết cây */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.plant.id = :plantId")
    Double averageRatingByPlantId(@Param("plantId") UUID plantId);

    /** Tổng số review của 1 cây */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.plant.id = :plantId")
    long countByPlantId(@Param("plantId") UUID plantId);
}
