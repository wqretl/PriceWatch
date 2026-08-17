package com.example.pricewatch.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryResponse(
        Long id,
        BigDecimal price,
        LocalDateTime checkedAt
) {
}
