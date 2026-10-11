package com.borrowbox.dto;

import java.math.BigDecimal;

public record AdminSummary(long totalUsers, long totalItems, long activeRentals,
                           long completedRentals, long pendingRequests, long totalPayments,
                           BigDecimal successfulPaymentAmount) {}
