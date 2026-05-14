package com.caycanh.caycanh_backend.config;

import com.caycanh.caycanh_backend.entity.Order;
import com.caycanh.caycanh_backend.repository.OrderRepository;
import com.caycanh.caycanh_backend.repository.RentalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Các task tự động chạy theo lịch.
 * Cần @EnableScheduling (đã có trong SchedulingConfig).
 */
@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final RentalRepository rentalRepository;
    private final OrderRepository orderRepository;

    public ScheduledTasks(RentalRepository rentalRepository,
                          OrderRepository orderRepository) {
        this.rentalRepository = rentalRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Mỗi ngày 1h sáng: rental active quá hạn → overdue.
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Ho_Chi_Minh")
    public void markOverdueRentals() {
        int count = rentalRepository.markOverdue(LocalDate.now());
        if (count > 0) {
            log.info("[ScheduledTasks] Đã đánh dấu {} rental quá hạn (overdue)", count);
        }
    }

    /**
     * Mỗi giờ: đơn chờ cọc/thanh toán quá 24h → tự hủy.
     * Đơn ở awaiting_deposit/awaiting_payment chưa từng confirmed
     * nên chưa trừ tồn kho → hủy đơn giản, không cần hoàn kho.
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void autoCancelExpiredOrders() {
        List<Order> expired = orderRepository.findExpiredAwaitingPayment(OffsetDateTime.now());
        for (Order order : expired) {
            order.setStatus(Order.OrderStatus.cancelled);
            order.setNote(appendNote(order.getNote(),
                    "[Tự động hủy: quá 24h chưa thanh toán]"));
            orderRepository.save(order);
        }
        if (!expired.isEmpty()) {
            log.info("[ScheduledTasks] Đã tự hủy {} đơn quá hạn thanh toán", expired.size());
        }
    }

    private String appendNote(String existing, String addition) {
        if (existing == null || existing.isBlank()) return addition;
        return existing + " " + addition;
    }
}
