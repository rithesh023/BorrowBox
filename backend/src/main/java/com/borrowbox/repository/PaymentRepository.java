package com.borrowbox.repository;

import com.borrowbox.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRentalId(Long rentalId);
    java.util.List<Payment> findByRental_Borrower_EmailIgnoreCaseOrderByCreatedAtDesc(String email);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = com.borrowbox.entity.PaymentStatus.SUCCESS")
    java.math.BigDecimal totalSuccessfulAmount();
}
