package com.borrowbox.dto;

import com.borrowbox.entity.Payment;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(Long id, Long rentalId, BigDecimal amount, String method,
                              String status, String reference, Instant createdAt) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getRental().getId(), payment.getAmount(),
                payment.getMethod().name(), payment.getStatus().name(), payment.getReference(), payment.getCreatedAt());
    }
}
