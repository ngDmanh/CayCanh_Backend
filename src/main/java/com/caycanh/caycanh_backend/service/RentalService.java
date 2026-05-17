package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.rental.*;
import com.caycanh.caycanh_backend.entity.*;
import com.caycanh.caycanh_backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class RentalService {

    private final RentalRepository rentalRepository;
    private final OrderRepository orderRepository;
    private final PlantRepository plantRepository;
    private final NotificationService notificationService;

    public RentalService(RentalRepository rentalRepository,
                         OrderRepository orderRepository,
                         PlantRepository plantRepository,
                         NotificationService notificationService) {
        this.rentalRepository = rentalRepository;
        this.orderRepository = orderRepository;
        this.plantRepository = plantRepository;
        this.notificationService = notificationService;
    }

    // ── CUSTOMER: xem rental của mình ─────────────────────────

    @Transactional(readOnly = true)
    public Page<RentalResponse> getMyRentals(User user, Pageable pageable) {
        return rentalRepository.findByUserId(user.getId(), pageable)
                .map(r -> toResponse(r, false));
    }

    @Transactional(readOnly = true)
    public RentalResponse getMyRentalById(User user, UUID rentalId) {
        Rental rental = findOrThrow(rentalId);
        if (!rental.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Không tìm thấy hợp đồng thuê: " + rentalId);
        }
        return toResponse(rental, false);
    }

    // ── ADMIN: xem tất cả rental ───────────────────────────────

    @Transactional(readOnly = true)
    public Page<RentalResponse> getAllRentals(Rental.RentalStatus status, Pageable pageable) {
        return rentalRepository.findByStatus(status, pageable)
                .map(r -> toResponse(r, true));
    }

    // ── ADMIN: đánh dấu đã giao cây ────────────────────────────

    /**
     * Admin giao cây cho khách → set start_date = today, tính end_date.
     * Status: pending_delivery → active.
     */
    @Transactional
    public RentalResponse markAsDelivered(UUID rentalId) {
        Rental rental = findOrThrow(rentalId);

        if (rental.getStatus() != Rental.RentalStatus.pending_delivery) {
            throw new IllegalArgumentException(
                    "Chỉ rental ở trạng thái pending_delivery mới đánh dấu giao được. " +
                    "Hiện tại: " + rental.getStatus()
            );
        }

        LocalDate today = LocalDate.now();
        rental.setStartDate(today);
        rental.setEndDate(rental.computeEndDate(today));
        rental.setStatus(Rental.RentalStatus.active);

        rentalRepository.save(rental);
        notificationService.notifyRentalActive(rental);
        return toResponse(rental, true);
    }

    // ── ADMIN: thu hồi cây ─────────────────────────────────────

    /**
     * Admin xác nhận đã thu hồi cây từ khách.
     * Cho phép thu hồi từ active hoặc overdue.
     * Trigger DB tự cộng lại rent_available_qty.
     */
    @Transactional
    public RentalResponse markAsCollected(UUID rentalId, CollectRentalRequest req) {
        Rental rental = findOrThrow(rentalId);

        if (rental.getStatus() != Rental.RentalStatus.active
                && rental.getStatus() != Rental.RentalStatus.overdue) {
            throw new IllegalArgumentException(
                    "Chỉ thu hồi được rental đang active hoặc overdue. " +
                    "Hiện tại: " + rental.getStatus()
            );
        }

        rental.setStatus(Rental.RentalStatus.returned);
        rental.setActualReturnDate(LocalDate.now());
        if (req.conditionOnReturn() != null && !req.conditionOnReturn().isBlank()) {
            rental.setConditionOnReturn(req.conditionOnReturn());
        }

        rentalRepository.save(rental);
        return toResponse(rental, true);
    }

    // ── CUSTOMER: gia hạn ──────────────────────────────────────

    /**
     * Khách bấm gia hạn — tạo Rental mới link parent.
     * Tính giá theo giá hiện tại của cây (không phải giá rental cũ).
     * Tạo Order rental mới cho khách thanh toán → khi paid, rental mới active.
     *
     * QUAN TRỌNG: rental cũ vẫn active cho đến khi end_date.
     * Rental mới có start_date = end_date của rental cũ + 1 ngày
     * → sẽ chuyển active khi admin set start_date thực tế (giao thêm gì đó)
     * Hoặc auto-active khi đến ngày (nếu cây vẫn đang ở khách).
     *
     * Vì cây vẫn đang ở chỗ khách, không cần giao lại — mình set luôn
     * status pending_delivery để admin xác nhận đã nhận tiền rồi mới active.
     */
    @Transactional
    public ExtendRentalResponse extendRental(User user, UUID rentalId, ExtendRentalRequest req) {
        Rental oldRental = findOrThrow(rentalId);

        // Validate ownership
        if (!oldRental.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Không tìm thấy hợp đồng thuê: " + rentalId);
        }

        // Chỉ cho gia hạn khi rental active (đang trong thời hạn)
        if (oldRental.getStatus() != Rental.RentalStatus.active) {
            throw new IllegalArgumentException(
                    "Chỉ gia hạn được khi rental đang active. " +
                    "Hiện tại: " + oldRental.getStatus()
            );
        }

        Plant plant = oldRental.getPlant();

        // Lấy giá hiện tại
        BigDecimal unitPrice = plant.getRentPrice(req.durationUnit());
        if (unitPrice == null) {
            throw new IllegalArgumentException(
                    "Cây này không có giá cho khung " + req.durationUnit() + " hiện tại"
            );
        }
        BigDecimal totalFee = unitPrice.multiply(BigDecimal.valueOf(req.duration()));

        // Tạo Order rental mới (đơn cọc 100% cho phần gia hạn)
        Order extensionOrder = Order.builder()
                .user(user)
                .orderType(Order.OrderType.rental)
                .status(Order.OrderStatus.pending)
                .paymentStatus(Order.PaymentStatus.unpaid)
                .recipientName(user.getFullName())
                .recipientPhone(user.getPhone() != null ? user.getPhone() : "0000000000")
                .recipientEmail(user.getEmail())
                .totalAmount(totalFee)
                .shippingAddress("Gia hạn — cây đã ở chỗ khách")
                .note("Gia hạn rental #" + oldRental.getId())
                .items(new ArrayList<>())
                .build();
        Order savedOrder = orderRepository.save(extensionOrder);

        // Tạo OrderItem cho đơn gia hạn
        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .plant(plant)
                .quantity(1)
                .unitPrice(unitPrice)
                .subtotal(totalFee)
                .build();
        savedOrder.getItems().add(orderItem);
        orderRepository.save(savedOrder);

        // Tạo Rental mới — pending_delivery, sẽ chuyển active khi admin xác nhận thanh toán
        Rental newRental = Rental.builder()
                .orderItem(orderItem)
                .user(user)
                .plant(plant)
                .duration(req.duration())
                .durationUnit(req.durationUnit())
                .totalRentalFee(totalFee)
                .status(Rental.RentalStatus.pending_delivery)
                .parentRental(oldRental)
                .build();
        rentalRepository.save(newRental);

        return new ExtendRentalResponse(
                newRental.getId(),
                oldRental.getId(),
                savedOrder.getId(),
                totalFee,
                "Đã tạo yêu cầu gia hạn. Vui lòng chuyển khoản " + totalFee + "₫ " +
                        "Liên hệ Zalo: 0982699028."
        );
    }

    // ── Helpers ────────────────────────────────────────────────

    private Rental findOrThrow(UUID rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hợp đồng thuê: " + rentalId));
    }

    private RentalResponse toResponse(Rental r, boolean includeCustomer) {
        Plant plant = r.getPlant();
        String primaryImage = plant.getImages() == null || plant.getImages().isEmpty()
                ? null
                : plant.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .orElse(plant.getImages().get(0))
                    .getImageUrl();

        RentalResponse.CustomerInfo customer = null;
        if (includeCustomer) {
            User u = r.getUser();
            customer = new RentalResponse.CustomerInfo(
                    u.getId(), u.getFullName(), u.getEmail(), u.getPhone()
            );
        }

        return new RentalResponse(
                r.getId(),
                plant.getId(),
                plant.getName(),
                primaryImage,
                r.getDuration(),
                r.getDurationUnit().name(),
                r.getStartDate() == null ? null : r.getStartDate().toString(),
                r.getEndDate() == null ? null : r.getEndDate().toString(),
                r.getStatus().name(),
                r.getTotalRentalFee(),
                r.getActualReturnDate() == null ? null : r.getActualReturnDate().toString(),
                r.getConditionOnReturn(),
                r.getParentRental() == null ? null : r.getParentRental().getId(),
                r.getCreatedAt(),
                customer
        );
    }
}
