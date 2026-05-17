package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.dto.stats.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Đọc dữ liệu từ 6 views có sẵn trong database.
 *
 * Dùng JdbcTemplate vì:
 * - Views không phải Entity JPA — không có @Id, không cần CRUD
 * - Native SQL đơn giản hơn JPQL khi map từ view sang DTO
 */
@Repository
public class StatsRepository {

    private final JdbcTemplate jdbc;

    @Autowired
    public StatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RevenueMonthlyResponse> getRevenueMonthly() {
        return jdbc.query(
            "SELECT month, order_type, total_orders, revenue " +
            "FROM v_revenue_monthly ORDER BY month DESC, order_type",
            (rs, i) -> new RevenueMonthlyResponse(
                rs.getTimestamp("month").toInstant().atOffset(ZoneOffset.UTC),
                rs.getString("order_type"),
                rs.getLong("total_orders"),
                rs.getBigDecimal("revenue")
            )
        );
    }

    public List<RevenueByTypeResponse> getRevenueByType() {
        return jdbc.query(
            "SELECT order_type, total_orders, total_revenue, avg_order_value " +
            "FROM v_revenue_by_type",
            (rs, i) -> new RevenueByTypeResponse(
                rs.getString("order_type"),
                rs.getLong("total_orders"),
                rs.getBigDecimal("total_revenue"),
                rs.getBigDecimal("avg_order_value")
            )
        );
    }

    public List<TopPlantResponse> getTopPlants(int limit) {
        return jdbc.query(
            "SELECT id, name, listing_type, total_quantity, total_revenue, " +
            "       avg_rating, review_count " +
            "FROM v_top_plants ORDER BY total_revenue DESC LIMIT ?",
            (rs, i) -> new TopPlantResponse(
                (UUID) rs.getObject("id"),
                rs.getString("name"),
                rs.getString("listing_type"),
                rs.getLong("total_quantity"),
                rs.getBigDecimal("total_revenue"),
                rs.getBigDecimal("avg_rating"),
                rs.getLong("review_count")
            ),
            limit
        );
    }

    public List<ActiveRentalResponse> getActiveRentals() {
        return jdbc.query(
            "SELECT rental_id, customer_name, customer_phone, plant_name, " +
            "       start_date, end_date, days_remaining, total_rental_fee, " +
            "       status, urgency " +
            "FROM v_active_rentals ORDER BY days_remaining ASC",
            (rs, i) -> new ActiveRentalResponse(
                (UUID) rs.getObject("rental_id"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                rs.getString("plant_name"),
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class),
                rs.getObject("days_remaining", Integer.class),
                rs.getBigDecimal("total_rental_fee"),
                rs.getString("status"),
                rs.getString("urgency")
            )
        );
    }

    public List<LowStockResponse> getLowStock() {
        return jdbc.query(
            "SELECT id, name, listing_type, stock_quantity, rent_available_qty, " +
            "       category_name " +
            "FROM v_low_stock",
            (rs, i) -> new LowStockResponse(
                (UUID) rs.getObject("id"),
                rs.getString("name"),
                rs.getString("listing_type"),
                rs.getObject("stock_quantity", Integer.class),
                rs.getObject("rent_available_qty", Integer.class),
                rs.getString("category_name")
            )
        );
    }

    public List<OrderSummaryResponse> getOrderSummary(String status, int limit, int offset) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, order_type, status, payment_status, total_amount, " +
            "       created_at, customer_name, customer_phone, item_count " +
            "FROM v_order_summary "
        );
        if (status != null && !status.isBlank()) {
            sql.append("WHERE status = ? ");
            sql.append("ORDER BY created_at DESC LIMIT ? OFFSET ?");
            return jdbc.query(sql.toString(),
                (rs, i) -> mapOrderSummary(rs),
                status, limit, offset);
        } else {
            sql.append("ORDER BY created_at DESC LIMIT ? OFFSET ?");
            return jdbc.query(sql.toString(),
                (rs, i) -> mapOrderSummary(rs),
                limit, offset);
        }
    }

    private OrderSummaryResponse mapOrderSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new OrderSummaryResponse(
            (UUID) rs.getObject("id"),
            rs.getString("order_type"),
            rs.getString("status"),
            rs.getString("payment_status"),
            rs.getBigDecimal("total_amount"),
            rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
            rs.getString("customer_name"),
            rs.getString("customer_phone"),
            rs.getLong("item_count")
        );
    }
}
