package com.borrowbox.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "rental_id", unique = true)
    private Rental rental;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.SUCCESS;
    @Column(nullable = false, unique = true, length = 40)
    private String reference = UUID.randomUUID().toString();
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    protected Payment() {}
    public Payment(Rental rental, BigDecimal amount, PaymentMethod method) { this.rental = rental; this.amount = amount; this.method = method; }
    public Long getId() { return id; }
    public Rental getRental() { return rental; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getReference() { return reference; }
    public Instant getCreatedAt() { return createdAt; }
}
