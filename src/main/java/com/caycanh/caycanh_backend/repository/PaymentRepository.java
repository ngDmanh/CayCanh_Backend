package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
