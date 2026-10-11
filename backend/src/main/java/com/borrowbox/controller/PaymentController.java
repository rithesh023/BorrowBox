package com.borrowbox.controller;

import com.borrowbox.dto.PaymentResponse;
import com.borrowbox.repository.PaymentRepository;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentRepository payments;
    public PaymentController(PaymentRepository payments) { this.payments = payments; }
    @GetMapping("/my")
    public List<PaymentResponse> mine(Authentication auth) {
        return payments.findByRental_Borrower_EmailIgnoreCaseOrderByCreatedAtDesc(auth.getName()).stream().map(PaymentResponse::from).toList();
    }
}
