package com.borrowbox.dto;

import java.math.BigDecimal;

public record ChatIntent(String intent, String query, String category, BigDecimal maxPricePerDay, Integer durationDays) {}
