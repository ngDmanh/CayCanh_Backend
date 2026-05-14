package com.caycanh.caycanh_backend.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin đánh dấu giao thất bại — bắt buộc ghi lý do.
 * Ví dụ: "Khách không nghe máy", "Địa chỉ sai", "Khách từ chối nhận".
 */
public record DeliveryFailedRequest(
        @NotBlank @Size(max = 500) String reason
) {}
