package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.stats.*;
import com.caycanh.caycanh_backend.repository.StatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service cho admin xem các báo cáo tổng hợp.
 * Tất cả query là READ-ONLY, không thay đổi DB.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private final StatsRepository statsRepository;

    public StatsService(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    public List<RevenueMonthlyResponse> getRevenueMonthly() {
        return statsRepository.getRevenueMonthly();
    }

    public List<RevenueByTypeResponse> getRevenueByType() {
        return statsRepository.getRevenueByType();
    }

    public List<TopPlantResponse> getTopPlants(int limit) {
        return statsRepository.getTopPlants(Math.min(Math.max(limit, 1), 50));
    }

    public List<ActiveRentalResponse> getActiveRentals() {
        return statsRepository.getActiveRentals();
    }

    public List<LowStockResponse> getLowStock() {
        return statsRepository.getLowStock();
    }

    public List<OrderSummaryResponse> getOrderSummary(String status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * safeSize;
        return statsRepository.getOrderSummary(status, safeSize, offset);
    }
}
