package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.config.OrderConstants;
import com.caycanh.caycanh_backend.dto.order.*;
import com.caycanh.caycanh_backend.entity.*;
import com.caycanh.caycanh_backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        RentalRepository rentalRepository,
                        PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.rentalRepository = rentalRepository;
        this.paymentRepository = paymentRepository;
    }

    // ════════════════════════════════════════════════════════════
    //  CHECKOUT — chuyển giỏ thành đơn, tự phân loại luồng A/B/C
    // ════════════════════════════════════════════════════════════

    @Transactional
    public CheckoutResponse checkout(User user, CheckoutRequest req) {
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng trống"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        List<CartItem> saleItems = cart.getItems().stream()
                .filter(i -> i.getItemType() == CartItem.ItemType.sale)
                .toList();
        List<CartItem> rentItems = cart.getItems().stream()
                .filter(i -> i.getItemType() == CartItem.ItemType.rent)
                .toList();

        validateStock(saleItems, rentItems);

        List<Order> createdOrders = new ArrayList<>();
        if (!saleItems.isEmpty()) {
            createdOrders.add(createSaleOrder(user, saleItems, req));
        }
        if (!rentItems.isEmpty()) {
            createdOrders.add(createRentalOrder(user, rentItems, req));
        }

        // Xóa giỏ sau khi tạo đơn
        cart.getItems().clear();
        cartRepository.save(cart);

        List<OrderResponse> responses = createdOrders.stream()
                .map(o -> toOrderResponse(o, false))
                .toList();

        String message = buildCheckoutMessage(createdOrders);
        return new CheckoutResponse(responses, message);
    }

    /**
     * Tạo đơn MUA — tự phân loại:
     *   - Tổng <= 500k → status = pending (COD bình thường, không cọc)
     *   - Tổng > 500k  → status = awaiting_deposit (chờ cọc 50%)
     */
    private Order createSaleOrder(User user, List<CartItem> items, CheckoutRequest req) {
        Order order = baseOrder(user, req, Order.OrderType.sale);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : items) {
            Plant plant = cartItem.getPlant();
            BigDecimal unitPrice = plant.getPriceSale();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .plant(plant)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();
            order.getItems().add(orderItem);
            total = total.add(subtotal);
        }
        order.setTotalAmount(total);

        // Phân loại luồng A vs B
        if (total.compareTo(OrderConstants.DEPOSIT_THRESHOLD) > 0) {
            // Luồng B: đơn lớn → cần cọc 50%
            BigDecimal deposit = total
                    .multiply(OrderConstants.DEPOSIT_RATIO_SALE)
                    .setScale(0, RoundingMode.HALF_UP);
            order.setStatus(Order.OrderStatus.awaiting_deposit);
            order.setDepositRequired(deposit);
            order.setPaymentDeadline(
                    OffsetDateTime.now().plusHours(OrderConstants.PAYMENT_DEADLINE_HOURS)
            );
        } else {
            // Luồng A: đơn nhỏ → COD bình thường
            order.setStatus(Order.OrderStatus.pending);
            order.setDepositRequired(BigDecimal.ZERO);
        }

        return orderRepository.save(order);
    }

    /**
     * Tạo đơn THUÊ — luồng C:
     *   - Luôn awaiting_payment (chờ thanh toán 100%)
     *   - Rental tạo ở pending_delivery
     */
    private Order createRentalOrder(User user, List<CartItem> items, CheckoutRequest req) {
        Order order = baseOrder(user, req, Order.OrderType.rental);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : items) {
            Plant plant = cartItem.getPlant();
            int duration = cartItem.getDuration();
            Plant.RentDurationUnit unit = cartItem.getDurationUnit();
            int qty = cartItem.getQuantity();

            BigDecimal unitPrice = plant.getRentPrice(unit);
            BigDecimal subtotal = unitPrice
                    .multiply(BigDecimal.valueOf(duration))
                    .multiply(BigDecimal.valueOf(qty));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .plant(plant)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();
            order.getItems().add(orderItem);
            total = total.add(subtotal);
        }
        order.setTotalAmount(total);

        // Luồng C: thuê luôn phải thanh toán 100% trước
        order.setStatus(Order.OrderStatus.awaiting_payment);
        order.setDepositRequired(total);  // cọc = 100%
        order.setPaymentDeadline(
                OffsetDateTime.now().plusHours(OrderConstants.PAYMENT_DEADLINE_HOURS)
        );

        Order savedOrder = orderRepository.save(order);

        // Tạo Rental cho từng orderItem — status pending_delivery
        for (int idx = 0; idx < items.size(); idx++) {
            CartItem cartItem = items.get(idx);
            OrderItem orderItem = savedOrder.getItems().get(idx);

            Rental rental = Rental.builder()
                    .orderItem(orderItem)
                    .user(user)
                    .plant(orderItem.getPlant())
                    .duration(cartItem.getDuration())
                    .durationUnit(cartItem.getDurationUnit())
                    .totalRentalFee(orderItem.getSubtotal())
                    .status(Rental.RentalStatus.pending_delivery)
                    .build();
            rentalRepository.save(rental);
        }

        return savedOrder;
    }

    /** Khởi tạo Order với các field chung */
    private Order baseOrder(User user, CheckoutRequest req, Order.OrderType type) {
        return Order.builder()
                .user(user)
                .orderType(type)
                .paymentStatus(Order.PaymentStatus.unpaid)
                .recipientName(req.recipientName())
                .recipientPhone(req.recipientPhone())
                .recipientEmail(user.getEmail())
                .shippingAddress(req.shippingAddress())
                .note(req.note())
                .totalAmount(BigDecimal.ZERO)
                .depositRequired(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
    }

    private String buildCheckoutMessage(List<Order> orders) {
        StringBuilder sb = new StringBuilder();
        for (Order o : orders) {
            switch (o.getStatus()) {
                case pending -> sb.append("Đơn mua đã được tạo, admin sẽ xác nhận sớm. ");
                case awaiting_deposit -> sb.append(
                        "Đơn mua lớn — vui lòng chuyển khoản cọc "
                                + o.getDepositRequired() + "₫ và gửi bill qua Zalo: 0982699028 trong 24h, Đơn hàng của bạn chỉ được vận chuyển khi chúng tôi nhận đủ tiền cọc của đơn hàng. ");
                case awaiting_payment -> sb.append(
                        "Đơn thuê — vui lòng chuyển khoản 100% ("
                                + o.getDepositRequired() + "₫) và gửi bill qua Zalo: 0982699028 trong 24h, Đơn hàng của bạn chỉ được vận chuyển khi chúng tôi nhận đủ tiền của đơn hàng. ");
                default -> {}
            }
        }
        return sb.toString().trim();
    }

    private void validateStock(List<CartItem> saleItems, List<CartItem> rentItems) {
        for (CartItem item : saleItems) {
            Plant plant = item.getPlant();
            if (plant.getStockQuantity() == null || plant.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Cây '" + plant.getName() + "' không đủ hàng — chỉ còn "
                                + (plant.getStockQuantity() == null ? 0 : plant.getStockQuantity()));
            }
        }
        for (CartItem item : rentItems) {
            Plant plant = item.getPlant();
            if (plant.getRentAvailableQty() == null || plant.getRentAvailableQty() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Cây '" + plant.getName() + "' không đủ cho thuê — chỉ còn "
                                + (plant.getRentAvailableQty() == null ? 0 : plant.getRentAvailableQty()));
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  CUSTOMER — xem đơn của mình
    // ════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(User user, Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByUserId(user.getId(), status, pageable)
                .map(o -> toOrderResponse(o, false));
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(User user, UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId);
        }
        return toOrderResponse(order, false);
    }

    /** Khách tự hủy đơn — chỉ hủy được khi chưa giao */
    @Transactional
    public OrderResponse cancelMyOrder(User user, UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId);
        }

        // Chỉ cho hủy khi đơn chưa được giao
        boolean cancellable = switch (order.getStatus()) {
            case pending, awaiting_deposit, awaiting_payment, confirmed -> true;
            case delivering, completed, cancelled, delivery_failed -> false;
        };
        if (!cancellable) {
            throw new IllegalArgumentException(
                    "Không thể hủy đơn ở trạng thái '" + order.getStatus() + "'");
        }

        order.setStatus(Order.OrderStatus.cancelled);
        orderRepository.save(order);
        return toOrderResponse(order, false);
    }

    // ════════════════════════════════════════════════════════════
    //  ADMIN — quản lý đơn
    // ════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Order.OrderStatus status,
                                            Order.OrderType orderType,
                                            Order.PaymentStatus paymentStatus,
                                            Pageable pageable) {
        return orderRepository.findByFilters(status, orderType, paymentStatus, pageable)
                .map(o -> toOrderResponse(o, true));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        return toOrderResponse(findOrderOrThrow(orderId), true);
    }

    /**
     * Admin xác nhận đã nhận cọc / thanh toán (qua Zalo).
     * Áp dụng cho đơn ở awaiting_deposit hoặc awaiting_payment.
     * → chuyển sang confirmed, trigger DB trừ tồn kho.
     */
    @Transactional
    public OrderResponse confirmDeposit(UUID orderId, UUID adminId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != Order.OrderStatus.awaiting_deposit
                && order.getStatus() != Order.OrderStatus.awaiting_payment) {
            throw new IllegalArgumentException(
                    "Chỉ xác nhận cọc cho đơn đang chờ thanh toán. Hiện tại: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.confirmed);
        order.setDepositConfirmedAt(OffsetDateTime.now());
        order.setDepositConfirmedBy(adminId);

        // Tạo bản ghi Payment cho khoản cọc
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getDepositRequired())
                .method(Payment.PaymentMethod.bank_transfer)
                .status(Payment.PaymentStatus.success)
                .paidAt(OffsetDateTime.now())
                .build();
        paymentRepository.save(payment);

        // Đơn thuê thanh toán 100% → đánh dấu paid luôn
        if (order.getOrderType() == Order.OrderType.rental) {
            order.setPaymentStatus(Order.PaymentStatus.paid);
        }

        orderRepository.save(order);
        return toOrderResponse(order, true);
    }

    /**
     * Admin xác nhận đơn nhỏ COD (không cần cọc).
     * pending → confirmed.
     */
    @Transactional
    public OrderResponse confirmOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != Order.OrderStatus.pending) {
            throw new IllegalArgumentException(
                    "Chỉ xác nhận đơn ở trạng thái pending. Hiện tại: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.confirmed);
        orderRepository.save(order);
        return toOrderResponse(order, true);
    }

    /**
     * Admin bắt đầu đi giao. confirmed → delivering.
     */
    @Transactional
    public OrderResponse startDelivery(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != Order.OrderStatus.confirmed) {
            throw new IllegalArgumentException(
                    "Chỉ giao đơn đã confirmed. Hiện tại: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.delivering);
        orderRepository.save(order);
        return toOrderResponse(order, true);
    }

    /**
     * Admin hoàn thành đơn — đã giao + thu đủ tiền.
     * Bắt buộc paymentStatus = paid trước khi completed.
     */
    @Transactional
    public OrderResponse completeOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != Order.OrderStatus.delivering) {
            throw new IllegalArgumentException(
                    "Chỉ hoàn thành đơn đang giao (delivering). Hiện tại: " + order.getStatus());
        }

        // Với đơn COD/đơn lớn: phải thu nốt tiền mới completed
        if (order.getPaymentStatus() != Order.PaymentStatus.paid) {
            throw new IllegalArgumentException(
                    "Phải xác nhận đã thu đủ tiền (gọi /payment) trước khi hoàn thành đơn");
        }

        order.setStatus(Order.OrderStatus.completed);
        orderRepository.save(order);
        return toOrderResponse(order, true);
    }

    /**
     * Admin đánh dấu giao thất bại (bùng hàng).
     * Trigger DB hoàn tồn kho + tăng failed_delivery_count.
     */
    @Transactional
    public OrderResponse markDeliveryFailed(UUID orderId, String reason) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != Order.OrderStatus.confirmed
                && order.getStatus() != Order.OrderStatus.delivering) {
            throw new IllegalArgumentException(
                    "Chỉ đánh dấu thất bại cho đơn confirmed/delivering. Hiện tại: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.delivery_failed);
        order.setFailureReason(reason);
        order.setFailedAt(OffsetDateTime.now());
        orderRepository.save(order);
        return toOrderResponse(order, true);
    }

    /**
     * Admin xác nhận đã thu tiền (COD hoặc thu nốt phần còn lại).
     */
    @Transactional
    public OrderResponse markAsPaid(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getPaymentStatus() == Order.PaymentStatus.paid) {
            throw new IllegalArgumentException("Đơn này đã được thanh toán đủ");
        }
        if (order.getStatus() == Order.OrderStatus.cancelled
                || order.getStatus() == Order.OrderStatus.delivery_failed) {
            throw new IllegalArgumentException("Đơn đã hủy/thất bại, không thể thanh toán");
        }

        // Tính số tiền còn phải thu = tổng - đã cọc
        BigDecimal alreadyPaid = order.getDepositConfirmedAt() != null
                ? order.getDepositRequired()
                : BigDecimal.ZERO;
        BigDecimal remaining = order.getTotalAmount().subtract(alreadyPaid);

        Payment payment = Payment.builder()
                .order(order)
                .amount(remaining)
                .method(Payment.PaymentMethod.cash)
                .status(Payment.PaymentStatus.success)
                .paidAt(OffsetDateTime.now())
                .build();
        paymentRepository.save(payment);

        order.setPaymentStatus(Order.PaymentStatus.paid);
        orderRepository.save(order);
        return toOrderResponse(order, true);
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));
    }

    private OrderResponse toOrderResponse(Order order, boolean includeCustomer) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        OrderResponse.CustomerInfo customerInfo = null;
        if (includeCustomer) {
            User u = order.getUser();
            customerInfo = new OrderResponse.CustomerInfo(
                    u.getId(), u.getFullName(), u.getEmail(), u.getPhone());
        }

        return new OrderResponse(
                order.getId(),
                order.getOrderType().name(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getTotalAmount(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getRecipientEmail(),
                order.getShippingAddress(),
                order.getNote(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                customerInfo,
                items
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        Plant plant = item.getPlant();

        String primaryImage = plant.getImages() == null || plant.getImages().isEmpty()
                ? null
                : plant.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .orElse(plant.getImages().get(0))
                .getImageUrl();

        OrderItemResponse.RentalInfo rentalInfo = null;
        if (item.getRental() != null) {
            Rental r = item.getRental();
            rentalInfo = new OrderItemResponse.RentalInfo(
                    r.getId(),
                    r.getStartDate() == null ? null : r.getStartDate().toString(),
                    r.getEndDate() == null ? null : r.getEndDate().toString(),
                    r.getDuration(),
                    r.getStatus().name(),
                    r.getActualReturnDate() == null ? null : r.getActualReturnDate().toString()
            );
        }

        return new OrderItemResponse(
                item.getId(),
                plant.getId(),
                plant.getName(),
                primaryImage,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                rentalInfo
        );
    }
}
