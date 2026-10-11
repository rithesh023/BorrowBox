package com.borrowbox.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RentalRequest(@NotNull Long itemId, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
