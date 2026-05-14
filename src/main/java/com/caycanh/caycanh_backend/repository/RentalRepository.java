package com.caycanh.caycanh_backend.repository;

import com.caycanh.caycanh_backend.entity.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RentalRepository extends JpaRepository<Rental, UUID> {

    /** Customer: tất cả rental của mình (kể cả pending_delivery để theo dõi) */
    @Query("SELECT r FROM Rental r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    Page<Rental> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /** Admin: lọc theo status */
    @Query("SELECT r FROM Rental r WHERE " +
           "(:status IS NULL OR r.status = :status) " +
           "ORDER BY r.createdAt DESC")
    Page<Rental> findByStatus(@Param("status") Rental.RentalStatus status, Pageable pageable);

    /** Scheduled: tìm rental active đã quá end_date để đánh dấu overdue */
    @Query("SELECT r FROM Rental r WHERE r.status = 'active' AND r.endDate < :today")
    List<Rental> findExpiredActive(@Param("today") LocalDate today);

    /** Bulk update overdue — gọi từ scheduled job */
    @Modifying
    @Transactional
    @Query("UPDATE Rental r SET r.status = 'overdue' " +
           "WHERE r.status = 'active' AND r.endDate < :today")
    int markOverdue(@Param("today") LocalDate today);
}
