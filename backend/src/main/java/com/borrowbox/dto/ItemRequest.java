package com.borrowbox.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ItemRequest(
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 140) String name,
        @NotBlank @Size(max = 3000) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal pricePerDay,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal securityDeposit,
        @NotBlank @Size(max = 40) String itemCondition,
        @Size(max = 1000) String imageUrl,
        Boolean available) {}
