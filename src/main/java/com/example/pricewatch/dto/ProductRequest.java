package com.example.pricewatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.URL;

public record ProductRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,
        @NotBlank(message = "Product URL must not be blank")
        @URL(message = "Product URL must be a valid URL")
        @Size(max = 2000, message = "Product URL must not exceed 2000 characters")
        String productUrl,
        @NotNull(message = "Current price must not be null")
        @Positive(message = "Current price must be greater than zero")
        BigDecimal currentPrice,
        @NotNull(message = "Target price must not be null")
        @Positive(message = "Target price must be greater than zero")
        BigDecimal targetPrice,
        Boolean active
) {
}
