package com.caycanh.caycanh_backend.config;

import com.caycanh.caycanh_backend.entity.Order;
import com.caycanh.caycanh_backend.entity.Rental;
import com.caycanh.caycanh_backend.repository.OrderRepository;
import com.caycanh.caycanh_backend.repository.RentalRepository;
import com.caycanh.caycanh_backend.service.NotificationService;
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
    private final NotificationService notificationService;

    public ScheduledTasks(RentalRepository rentalRepository,
                          OrderRepository orderRepository,
                          NotificationService notificationService) {
        this.rentalRepository = rentalRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    /**
     * Mỗi ngày 1h sáng: rental active quá hạn → overdue.
     * Logic: SELECT trước để biết rental nào quá hạn (gửi notify),
     * sau đó mới UPDATE bulk.
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Ho_Chi_Minh")
    public void markOverdueRentals() {
        // 1. Tìm danh sách rental sắp bị đánh dấu overdue
        List<Rental> expired = rentalRepository.findExpiredActive(LocalDate.now());

        // 2. Gửi notification cho từng khách trước khi update
        for (Rental r : expired) {
            notificationService.notifyRentalOverdue(r);
        }

        // 3. Update bulk thành overdue
        int count = rentalRepository.markOverdue(LocalDate.now());

        // 4. Báo admin tổng số
        if (count > 0) {
            notificationService.notifyAdminOverdueRentals(count);
            log.info("[ScheduledTasks] Đã đánh dấu {} rental quá hạn", count);
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

            // Báo cho khách biết đơn của họ tự hủy
            notificationService.notifyOrderAutoCancelled(order);
        }
        if (!expired.isEmpty()) {
            notificationService.notifyAdminAutoCancelled(expired.size());
            log.info("[ScheduledTasks] Đã tự hủy {} đơn quá hạn thanh toán", expired.size());
        }
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional(readOnly = true)
    public void notifyExpiringSoonRentals() {
        LocalDate threshold = LocalDate.now().plusDays(3);
        List<Rental> expiringSoon = rentalRepository.findExpiringOn(threshold);
        for (Rental r : expiringSoon) {
            notificationService.notifyRentalExpiringSoon(r);
        }
        if (!expiringSoon.isEmpty()) {
            log.info("[ScheduledTasks] Đã thông báo {} rental sắp hết hạn", expiringSoon.size());
        }
    }

    private String appendNote(String existing, String addition) {
        if (existing == null || existing.isBlank()) return addition;
        return existing + " " + addition;
    }
}
