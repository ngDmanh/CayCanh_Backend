package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.notification.NotificationResponse;
import com.caycanh.caycanh_backend.entity.*;
import com.caycanh.caycanh_backend.repository.NotificationRepository;
import com.caycanh.caycanh_backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service trung tâm để tạo notification.
 *
 * Các Service khác (Order, Rental, Scheduled...) gọi vào đây mỗi khi
 * xảy ra sự kiện cần báo cho khách hoặc admin.
 *
 * Hệ thống có 1 admin duy nhất — getAdmin() tìm user đầu tiên role=admin
 * và cache cho lần gọi sau.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /** Cache user admin để không phải query DB mỗi lần gửi */
    private User cachedAdmin;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ════════════════════════════════════════════════════════════
    //  API CHO USER (đọc danh sách + đánh dấu đã đọc)
    // ════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserId(user.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc.
     * Gọi khi khách mở màn hình "Thông báo" (style Shopee).
     */
    @Transactional
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsRead(user.getId());
    }

    // ════════════════════════════════════════════════════════════
    //  HELPER — các Service khác gọi vào để tạo notification
    // ════════════════════════════════════════════════════════════

    // ── Thông báo cho khách ────────────────────────────────────

    /** Khách đặt hàng xong */
    public void notifyOrderCreated(User user, Order order) {
        String body = order.getOrderType() == Order.OrderType.rental
                ? "Đơn thuê #" + shortId(order.getId()) + " đã được tạo. " +
                  "Vui lòng chuyển khoản và gửi bill qua Zalo."
                : (order.getStatus() == Order.OrderStatus.awaiting_deposit
                    ? "Đơn mua #" + shortId(order.getId()) + " đã được tạo. " +
                      "Vui lòng chuyển khoản cọc 50% qua Zalo."
                    : "Đơn mua #" + shortId(order.getId()) + " đã được tạo. " +
                      "Admin sẽ liên hệ xác nhận sớm.");

        create(user, "Đặt hàng thành công", body,
               Notification.NotificationType.order, "orders", order.getId());
    }

    /** Admin xác nhận đơn (cọc hoặc đơn nhỏ) */
    public void notifyOrderConfirmed(Order order) {
        create(order.getUser(),
               "Đơn hàng đã được xác nhận",
               "Đơn #" + shortId(order.getId()) + " đã được admin xác nhận. " +
               "Cây của bạn sẽ được giao sớm.",
               Notification.NotificationType.order, "orders", order.getId());
    }

    /** Admin bắt đầu giao */
    public void notifyOrderDelivering(Order order) {
        create(order.getUser(),
               "Đang giao hàng",
               "Đơn #" + shortId(order.getId()) + " đang trên đường đến bạn. " +
               "Vui lòng giữ máy.",
               Notification.NotificationType.order, "orders", order.getId());
    }

    /** Đơn hoàn thành */
    public void notifyOrderCompleted(Order order) {
        create(order.getUser(),
               "Đơn hàng hoàn thành",
               "Cảm ơn bạn đã mua hàng! Đơn #" + shortId(order.getId()) + " đã được giao thành công. " +
               "Bạn có thể đánh giá cây trong phần lịch sử đơn hàng.",
               Notification.NotificationType.order, "orders", order.getId());
    }

    /** Đơn giao thất bại / khách bùng */
    public void notifyOrderFailed(Order order) {
        create(order.getUser(),
               "Giao hàng thất bại",
               "Đơn #" + shortId(order.getId()) + " không giao được. " +
               "Lý do: " + (order.getFailureReason() != null ? order.getFailureReason() : "Không rõ"),
               Notification.NotificationType.order, "orders", order.getId());
    }

    /** Đơn tự hủy do quá 24h không thanh toán */
    public void notifyOrderAutoCancelled(Order order) {
        create(order.getUser(),
               "Đơn hàng đã tự hủy",
               "Đơn #" + shortId(order.getId()) + " đã bị hủy do quá 24h không thanh toán. " +
               "Bạn có thể đặt lại nếu muốn.",
               Notification.NotificationType.order, "orders", order.getId());
    }

    /** Khách tự hủy đơn */
    public void notifyOrderCancelled(Order order) {
        create(order.getUser(),
                "Đã hủy đơn hàng",
                "Đơn #" + shortId(order.getId()) + " đã được hủy thành công theo yêu cầu của bạn.",
                Notification.NotificationType.order, "orders", order.getId());
    }

    /** Admin đã giao cây thuê (rental chuyển active) */
    public void notifyRentalActive(Rental rental) {
        create(rental.getUser(),
               "Cây thuê đã được giao",
               "Cây '" + rental.getPlant().getName() + "' đã được giao. " +
               "Hợp đồng thuê bắt đầu từ " + rental.getStartDate() +
               " đến " + rental.getEndDate() + ".",
               Notification.NotificationType.rental, "rentals", rental.getId());
    }

    /** Cây thuê sắp hết hạn (báo trước 3 ngày) — gọi từ scheduled job */
    public void notifyRentalExpiringSoon(Rental rental) {
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), rental.getEndDate()
        );
        create(rental.getUser(),
               "Cây thuê sắp hết hạn",
               "Cây '" + rental.getPlant().getName() + "' sẽ hết hạn thuê sau " + daysLeft + " ngày. " +
               "Bạn có thể bấm gia hạn trong app nếu muốn giữ cây thêm.",
               Notification.NotificationType.rental, "rentals", rental.getId());
    }

    /** Cây thuê quá hạn — báo khách */
    public void notifyRentalOverdue(Rental rental) {
        create(rental.getUser(),
               "Hợp đồng thuê đã quá hạn",
               "Cây '" + rental.getPlant().getName() + "' đã quá hạn thuê. " +
               "Admin sẽ liên hệ thu hồi cây.",
               Notification.NotificationType.rental, "rentals", rental.getId());
    }

    // ── Thông báo cho admin ────────────────────────────────────

    /** Có đơn hàng mới */
    public void notifyAdminNewOrder(Order order) {
        User admin = getAdmin();
        if (admin == null) return;

        String typeLabel = order.getOrderType() == Order.OrderType.rental ? "thuê" : "mua";
        String statusLabel = switch (order.getStatus()) {
            case awaiting_deposit -> " (chờ cọc 50%)";
            case awaiting_payment -> " (chờ thanh toán 100%)";
            default -> "";
        };

        create(admin,
               "Đơn " + typeLabel + " mới",
               "Có đơn " + typeLabel + " mới #" + shortId(order.getId()) +
               " từ " + order.getUser().getFullName() +
               " — tổng " + order.getTotalAmount() + "₫" + statusLabel,
               Notification.NotificationType.order, "orders", order.getId());
    }

    /** Báo admin biết có rental sắp/đã quá hạn (gộp số liệu) */
    public void notifyAdminOverdueRentals(int count) {
        if (count == 0) return;
        User admin = getAdmin();
        if (admin == null) return;

        create(admin,
               "Có " + count + " cây thuê quá hạn",
               "Có " + count + " hợp đồng thuê đã quá hạn. " +
               "Vui lòng kiểm tra danh sách và liên hệ khách để thu hồi cây.",
               Notification.NotificationType.rental, null, null);
    }

    /** Báo admin biết có đơn tự hủy do quá 24h */
    public void notifyAdminAutoCancelled(int count) {
        if (count == 0) return;
        User admin = getAdmin();
        if (admin == null) return;

        create(admin,
               "Có " + count + " đơn tự hủy",
               "Có " + count + " đơn hàng đã bị hủy tự động do khách không thanh toán trong 24h.",
               Notification.NotificationType.order, null, null);
    }

    /** Khách vừa hủy 1 đơn */
    public void notifyAdminCustomerCancelled(Order order) {
        User admin = getAdmin();
        if (admin == null) return;

        create(admin,
                "Khách đã hủy đơn",
                "Khách " + order.getUser().getFullName() +
                        " đã hủy đơn #" + shortId(order.getId()) + " — tổng " + order.getTotalAmount() + "₫",
                Notification.NotificationType.order, "orders", order.getId());
    }

    /**
     * Đánh dấu 1 thông báo cụ thể là đã đọc.
     * Gọi khi khách bấm vào thông báo trong danh sách.
     *
     * Bảo mật: chỉ đánh dấu được thông báo của chính mình.
     */
    @Transactional
    public void markOneAsRead(User user, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy thông báo: " + notificationId));

        // Chỉ cho đánh dấu thông báo của chính user
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "Không tìm thấy thông báo: " + notificationId);
        }

        // Nếu đã đọc rồi thì không cần update DB
        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    // ════════════════════════════════════════════════════════════
    //  Internal helpers
    // ════════════════════════════════════════════════════════════

    /** Tạo notification record — gọi từ tất cả các method notify ở trên */
    @Transactional
    public void create(User user, String title, String body,
                       Notification.NotificationType type,
                       String refType, UUID refId) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(type)
                .refType(refType)
                .refId(refId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    /** Tìm user admin (cache lại cho các lần gọi sau) */
    private User getAdmin() {
        if (cachedAdmin == null) {
            cachedAdmin = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.admin)
                    .findFirst()
                    .orElse(null);
        }
        return cachedAdmin;
    }

    /** Cắt UUID dài thành 8 ký tự đầu cho hiển thị */
    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getBody(),
                n.getType().name(),
                n.getRefType(),
                n.getRefId(),
                n.getIsRead(),
                n.getCreatedAt()
        );
    }
}
