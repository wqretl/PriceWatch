package com.example.pricewatch.service;

import com.example.pricewatch.entity.Product;
import java.math.BigDecimal;

public record PriceUpdateResult(
        Product product,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        boolean changed,
        boolean targetReached
) {
}
