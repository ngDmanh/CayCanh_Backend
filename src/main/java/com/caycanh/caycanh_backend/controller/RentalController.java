package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.rental.*;
import com.caycanh.caycanh_backend.entity.Rental;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    // ── Customer: rental của mình ──────────────────────────────

    @GetMapping("/api/rentals/my")
    public ResponseEntity<Page<RentalResponse>> getMyRentals(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(rentalService.getMyRentals(user, pageable));
    }

    @GetMapping("/api/rentals/my/{id}")
    public ResponseEntity<RentalResponse> getMyRentalById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(rentalService.getMyRentalById(user, id));
    }

    /** Gia hạn rental — tạo rental + order mới */
    @PostMapping("/api/rentals/{id}/extend")
    public ResponseEntity<ExtendRentalResponse> extendRental(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody ExtendRentalRequest req
    ) {
        return ResponseEntity.ok(rentalService.extendRental(user, id, req));
    }

    // ── Admin: quản lý rental ──────────────────────────────────

    @GetMapping("/api/admin/rentals")
    public ResponseEntity<Page<RentalResponse>> getAllRentals(
            @RequestParam(required = false) Rental.RentalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(rentalService.getAllRentals(status, pageable));
    }

    /** Đánh dấu đã giao cây → pending_delivery sang active */
    @PatchMapping("/api/admin/rentals/{id}/deliver")
    public ResponseEntity<RentalResponse> markAsDelivered(@PathVariable UUID id) {
        return ResponseEntity.ok(rentalService.markAsDelivered(id));
    }

    /** Đánh dấu đã thu hồi cây → active/overdue sang returned */
    @PatchMapping("/api/admin/rentals/{id}/collect")
    public ResponseEntity<RentalResponse> markAsCollected(
            @PathVariable UUID id,
            @Valid @RequestBody CollectRentalRequest req
    ) {
        return ResponseEntity.ok(rentalService.markAsCollected(id, req));
    }
}
