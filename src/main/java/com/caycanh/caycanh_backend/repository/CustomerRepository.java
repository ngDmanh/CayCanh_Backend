package com.caycanh.caycanh_backend.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Repository riêng cho customer mgmt — tận dụng SQL aggregate
 * thay vì load nhiều entity rồi đếm trong Java (chậm hơn nhiều).
 */
@Repository
public class CustomerRepository {

    private final JdbcTemplate jdbc;

    @Autowired
    public CustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Số liệu tổng hợp của 1 user — gọi 1 query duy nhất.
     * Trả về Map<column_name, value> để Service tự build DTO.
     */
    public Map<String, Object> getCustomerStats(UUID userId) {
        String sql = """
            SELECT
                COUNT(DISTINCT o.id)                                            AS total_orders,
                COUNT(DISTINCT CASE WHEN o.status = 'completed' THEN o.id END)  AS completed_orders,
                COUNT(DISTINCT CASE WHEN o.status = 'cancelled' THEN o.id END)  AS cancelled_orders,
                COUNT(DISTINCT CASE WHEN o.status = 'delivery_failed' THEN o.id END) AS failed_deliveries,
                COALESCE(SUM(CASE WHEN o.status = 'completed' THEN o.total_amount END), 0) AS total_spent,
                COUNT(DISTINCT CASE WHEN r.status IN ('active', 'overdue') THEN r.id END) AS active_rentals,
                MAX(o.created_at) AS last_order_at
            FROM users u
            LEFT JOIN orders o ON o.user_id = u.id
            LEFT JOIN rentals r ON r.user_id = u.id
            WHERE u.id = ?
            GROUP BY u.id
            """;

        return jdbc.queryForObject(sql, (rs, i) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("totalOrders", rs.getLong("total_orders"));
            map.put("completedOrders", rs.getLong("completed_orders"));
            map.put("cancelledOrders", rs.getLong("cancelled_orders"));
            map.put("failedDeliveries", rs.getLong("failed_deliveries"));
            map.put("totalSpent", rs.getBigDecimal("total_spent"));
            map.put("activeRentals", rs.getLong("active_rentals"));
            map.put("lastOrderAt", rs.getTimestamp("last_order_at") == null
                    ? null
                    : rs.getTimestamp("last_order_at").toInstant().atOffset(ZoneOffset.UTC));
            return map;
        }, userId);
    }

    /**
     * Số đơn completed của một user — dùng cho danh sách.
     * Gọi nhanh, không load detail.
     */
    public long countCompletedOrders(UUID userId) {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE user_id = ? AND status = 'completed'",
            Long.class, userId);
        return count == null ? 0 : count;
    }
}
