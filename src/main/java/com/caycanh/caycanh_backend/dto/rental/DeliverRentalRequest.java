package com.caycanh.caycanh_backend.dto.rental;

/**
 * Admin đánh dấu đã giao cây — không cần body.
 * Server tự set start_date = today, tính end_date theo duration.
 */
public record DeliverRentalRequest() {}
