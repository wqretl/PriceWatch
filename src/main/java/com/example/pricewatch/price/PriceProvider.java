package com.example.pricewatch.price;

import java.math.BigDecimal;

public interface PriceProvider {

    BigDecimal getPrice(String productUrl);
}
