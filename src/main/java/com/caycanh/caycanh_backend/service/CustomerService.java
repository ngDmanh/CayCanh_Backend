package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.customer.*;
import com.caycanh.caycanh_backend.dto.order.OrderResponse;
import com.caycanh.caycanh_backend.dto.rental.RentalResponse;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service quản trị khách hàng — chỉ đọc, không thay đổi.
 *
 * Admin có thể:
 *  - Xem danh sách khách + tìm kiếm
 *  - Xem chi tiết 1 khách + số liệu tổng hợp
 *  - Xem lịch sử đơn hàng / rental của khách
 */
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;
    private final RentalService rentalService;

    public CustomerService(UserRepository userRepository,
                           CustomerRepository customerRepository,
                           OrderService orderService,
                           RentalService rentalService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.orderService = orderService;
        this.rentalService = rentalService;
    }

    /**
     * Danh sách khách hàng, có tìm kiếm theo tên/email/sđt.
     * Mỗi dòng kèm số đơn completed + số lần bùng hàng.
     */
    public Page<CustomerListItemResponse> getCustomers(String search, Pageable pageable) {
        return userRepository.searchCustomers(search, pageable)
                .map(this::toListItem);
    }

    /**
     * Chi tiết 1 khách + số liệu tổng hợp.
     */
    public CustomerDetailResponse getCustomerDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng: " + userId));

        if (user.getRole() != User.Role.customer) {
            throw new IllegalArgumentException("User này không phải khách hàng");
        }

        Map<String, Object> stats = customerRepository.getCustomerStats(userId);

        return new CustomerDetailResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getIsActive(),
                user.getCreatedAt(),
                (Long) stats.get("totalOrders"),
                (Long) stats.get("completedOrders"),
                (Long) stats.get("cancelledOrders"),
                (Long) stats.get("failedDeliveries"),
                (BigDecimal) stats.get("totalSpent"),
                (Long) stats.get("activeRentals"),
                (OffsetDateTime) stats.get("lastOrderAt")
        );
    }

    /**
     * Lịch sử đơn của 1 khách — tận dụng OrderService có sẵn.
     */
    public Page<OrderResponse> getCustomerOrders(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng: " + userId));
        return orderService.getMyOrders(user, null, pageable);
    }

    /**
     * Lịch sử rental của 1 khách.
     */
    public Page<RentalResponse> getCustomerRentals(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng: " + userId));
        return rentalService.getMyRentals(user, pageable);
    }

    // ── Helpers ────────────────────────────────────────────────

    private CustomerListItemResponse toListItem(User user) {
        long completedOrders = customerRepository.countCompletedOrders(user.getId());
        return new CustomerListItemResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getIsActive(),
                user.getCreatedAt(),
                completedOrders,
                user.getFailedDeliveryCount() == null ? 0 : user.getFailedDeliveryCount()
        );
    }
}
