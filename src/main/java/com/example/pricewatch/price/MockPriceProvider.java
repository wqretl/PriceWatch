package com.example.pricewatch.price;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock-price")
public class MockPriceProvider implements PriceProvider {

    @Override
    public BigDecimal getPrice(String productUrl) {
        long priceInCents = ThreadLocalRandom.current().nextLong(100_000, 10_000_001);
        return BigDecimal.valueOf(priceInCents, 2);
    }
}
