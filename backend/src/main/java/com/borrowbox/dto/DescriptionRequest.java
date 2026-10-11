package com.borrowbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DescriptionRequest(@NotBlank @Size(max = 140) String name,
                                 @NotBlank @Size(max = 40) String condition,
                                 @Size(max = 3000) String details) {}
