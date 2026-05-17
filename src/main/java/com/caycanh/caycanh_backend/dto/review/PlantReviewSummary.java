package com.caycanh.caycanh_backend.dto.review;

/**
 * Tóm tắt đánh giá của 1 cây — điểm trung bình + tổng số review.
 * Dùng cho trang chi tiết cây hiển thị "4.5 sao (12 đánh giá)".
 */
public record PlantReviewSummary(
        Double averageRating,   // null nếu chưa có review
        long totalReviews
) {}
