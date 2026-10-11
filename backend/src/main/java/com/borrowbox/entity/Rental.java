package com.borrowbox.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "rentals", indexes = @Index(name = "idx_rental_dates", columnList = "item_id,start_date,end_date,status"))
public class Rental {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "item_id")
    private Item item;
    @ManyToOne(optional = false) @JoinColumn(name = "borrower_id")
    private AppUser borrower;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rentalAmount;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal securityDeposit;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RentalStatus status = RentalStatus.PENDING;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Rental() {}
    public Rental(Item item, AppUser borrower, LocalDate startDate, LocalDate endDate,
                  BigDecimal rentalAmount, BigDecimal securityDeposit, BigDecimal totalAmount) {
        this.item = item; this.borrower = borrower; this.startDate = startDate; this.endDate = endDate;
        this.rentalAmount = rentalAmount; this.securityDeposit = securityDeposit; this.totalAmount = totalAmount;
    }
    public void setStatus(RentalStatus status) { this.status = status; }
    public Long getId() { return id; }
    public Item getItem() { return item; }
    public AppUser getBorrower() { return borrower; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getRentalAmount() { return rentalAmount; }
    public BigDecimal getSecurityDeposit() { return securityDeposit; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public RentalStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
