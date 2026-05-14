package com.caycanh.caycanh_backend.dto.order;

/**
 * Admin xác nhận đã thu tiền COD.
 * Không cần body field nào - chỉ cần gọi endpoint là đánh dấu paid.
 * Để rỗng cho mở rộng tương lai (ghi chú thu tiền, người thu...)
 */
public record UpdatePaymentRequest() {}
