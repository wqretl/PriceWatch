package com.example.pricewatch.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String productUrl,
        BigDecimal currentPrice,
        BigDecimal targetPrice,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
