package com.example.pricewatch.scheduler;

import com.example.pricewatch.service.PriceUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceCheckScheduler {

    private final PriceUpdateService priceUpdateService;

    @Scheduled(fixedDelayString = "${pricewatch.scheduler.fixed-delay}")
    public void checkPrices() {
        priceUpdateService.updateAllActiveProducts();
    }
}
