package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.stats.*;
import com.caycanh.caycanh_backend.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** Doanh thu theo tháng × loại */
    @GetMapping("/revenue-monthly")
    public ResponseEntity<List<RevenueMonthlyResponse>> getRevenueMonthly() {
        return ResponseEntity.ok(statsService.getRevenueMonthly());
    }

    /** Tổng doanh thu chia theo mua/thuê */
    @GetMapping("/revenue-by-type")
    public ResponseEntity<List<RevenueByTypeResponse>> getRevenueByType() {
        return ResponseEntity.ok(statsService.getRevenueByType());
    }

    /** Top cây bán/cho thuê tốt nhất */
    @GetMapping("/top-plants")
    public ResponseEntity<List<TopPlantResponse>> getTopPlants(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(statsService.getTopPlants(limit));
    }

    /** Rental đang active — kèm cảnh báo gần hết hạn */
    @GetMapping("/active-rentals")
    public ResponseEntity<List<ActiveRentalResponse>> getActiveRentals() {
        return ResponseEntity.ok(statsService.getActiveRentals());
    }

    /** Cây sắp hết hàng — admin biết để nhập thêm */
    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockResponse>> getLowStock() {
        return ResponseEntity.ok(statsService.getLowStock());
    }

    /** Tổng quan đơn hàng — bảng đơn nhanh */
    @GetMapping("/order-summary")
    public ResponseEntity<List<OrderSummaryResponse>> getOrderSummary(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(statsService.getOrderSummary(status, page, size));
    }
}
