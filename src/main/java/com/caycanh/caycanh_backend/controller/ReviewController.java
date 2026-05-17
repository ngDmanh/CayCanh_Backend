package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.review.*;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // ── Public: xem review của cây (không cần đăng nhập) ───────

    @GetMapping("/api/plants/{plantId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getPlantReviews(
            @PathVariable UUID plantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getPlantReviews(plantId, pageable));
    }

    @GetMapping("/api/plants/{plantId}/reviews/summary")
    public ResponseEntity<PlantReviewSummary> getPlantReviewSummary(@PathVariable UUID plantId) {
        return ResponseEntity.ok(reviewService.getPlantReviewSummary(plantId));
    }

    // ── Customer: quản lý review của mình ──────────────────────

    /** Tạo đánh giá mới */
    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateReviewRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(user, req));
    }

    /** Xem các đánh giá mình đã viết */
    @GetMapping("/api/reviews/my")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getMyReviews(user, pageable));
    }

    /** Sửa đánh giá của mình */
    @PutMapping("/api/reviews/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequest req
    ) {
        return ResponseEntity.ok(reviewService.updateReview(user, id, req));
    }

    /** Xóa đánh giá của mình */
    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        reviewService.deleteReview(user, id);
        return ResponseEntity.noContent().build();
    }
}
