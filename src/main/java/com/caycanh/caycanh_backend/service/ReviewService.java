package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.review.*;
import com.caycanh.caycanh_backend.entity.*;
import com.caycanh.caycanh_backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final PlantRepository plantRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         OrderRepository orderRepository,
                         PlantRepository plantRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.plantRepository = plantRepository;
    }

    // ── CUSTOMER: tạo review ───────────────────────────────────

    /**
     * Tạo đánh giá. Quy tắc kiểm tra:
     *  1. Order phải tồn tại và thuộc về user này
     *  2. Order phải ở trạng thái completed
     *  3. Cây phải thực sự nằm trong đơn đó
     *  4. Chưa từng review cây này trong đơn này (unique constraint)
     */
    @Transactional
    public ReviewResponse createReview(User user, CreateReviewRequest req) {
        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + req.orderId()));

        // Quy tắc 1: đơn phải thuộc về user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Đơn hàng này không thuộc về bạn");
        }

        // Quy tắc 2: chỉ review khi đơn đã completed
        if (order.getStatus() != Order.OrderStatus.completed) {
            throw new IllegalArgumentException(
                    "Chỉ đánh giá được khi đơn hàng đã hoàn thành. Trạng thái hiện tại: "
                    + order.getStatus());
        }

        // Quy tắc 3: cây phải nằm trong đơn này
        boolean plantInOrder = order.getItems().stream()
                .anyMatch(item -> item.getPlant().getId().equals(req.plantId()));
        if (!plantInOrder) {
            throw new IllegalArgumentException("Cây này không có trong đơn hàng đã chọn");
        }

        // Quy tắc 4: chưa review cây này trong đơn này
        boolean alreadyReviewed = reviewRepository.existsByUserIdAndPlantIdAndOrderId(
                user.getId(), req.plantId(), req.orderId());
        if (alreadyReviewed) {
            throw new IllegalArgumentException("Bạn đã đánh giá cây này trong đơn hàng này rồi");
        }

        Plant plant = plantRepository.findById(req.plantId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cây: " + req.plantId()));

        Review review = Review.builder()
                .user(user)
                .plant(plant)
                .order(order)
                .rating(req.rating())
                .comment(req.comment())
                .build();

        return toResponse(reviewRepository.save(review));
    }

    // ── CUSTOMER: sửa review ───────────────────────────────────

    @Transactional
    public ReviewResponse updateReview(User user, UUID reviewId, UpdateReviewRequest req) {
        Review review = findOrThrow(reviewId);

        // Chỉ sửa được review của chính mình
        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Không tìm thấy đánh giá: " + reviewId);
        }

        review.setRating(req.rating());
        review.setComment(req.comment());

        return toResponse(reviewRepository.save(review));
    }

    // ── CUSTOMER: xóa review ───────────────────────────────────

    @Transactional
    public void deleteReview(User user, UUID reviewId) {
        Review review = findOrThrow(reviewId);

        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Không tìm thấy đánh giá: " + reviewId);
        }

        reviewRepository.delete(review);
    }

    // ── CUSTOMER: xem review của mình ──────────────────────────

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(User user, Pageable pageable) {
        return reviewRepository.findByUserId(user.getId(), pageable)
                .map(this::toResponse);
    }

    // ── PUBLIC: xem review của 1 cây ───────────────────────────

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getPlantReviews(UUID plantId, Pageable pageable) {
        return reviewRepository.findByPlantId(plantId, pageable)
                .map(this::toResponse);
    }

    /** Tóm tắt: điểm trung bình + tổng số review của cây */
    @Transactional(readOnly = true)
    public PlantReviewSummary getPlantReviewSummary(UUID plantId) {
        Double avg = reviewRepository.averageRatingByPlantId(plantId);
        long total = reviewRepository.countByPlantId(plantId);
        // Làm tròn 1 chữ số thập phân
        Double rounded = avg == null ? null : Math.round(avg * 10.0) / 10.0;
        return new PlantReviewSummary(rounded, total);
    }

    // ── Helpers ────────────────────────────────────────────────

    private Review findOrThrow(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá: " + reviewId));
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getPlant().getId(),
                r.getPlant().getName(),
                r.getOrder().getId(),
                r.getUser().getId(),
                r.getUser().getFullName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
