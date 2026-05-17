package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.customer.*;
import com.caycanh.caycanh_backend.dto.order.OrderResponse;
import com.caycanh.caycanh_backend.dto.rental.RentalResponse;
import com.caycanh.caycanh_backend.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final CustomerService customerService;

    public AdminCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /** Danh sách khách hàng, có tìm kiếm theo tên/email/sđt */
    @GetMapping
    public ResponseEntity<Page<CustomerListItemResponse>> getCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(customerService.getCustomers(search, pageable));
    }

    /** Chi tiết 1 khách + số liệu tổng hợp */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailResponse> getCustomerDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomerDetail(id));
    }

    /** Lịch sử đơn của khách */
    @GetMapping("/{id}/orders")
    public ResponseEntity<Page<OrderResponse>> getCustomerOrders(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(customerService.getCustomerOrders(id, pageable));
    }

    /** Lịch sử rental của khách */
    @GetMapping("/{id}/rentals")
    public ResponseEntity<Page<RentalResponse>> getCustomerRentals(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(customerService.getCustomerRentals(id, pageable));
    }
}
