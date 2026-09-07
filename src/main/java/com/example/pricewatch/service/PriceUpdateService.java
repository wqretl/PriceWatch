package com.example.pricewatch.service;

import com.example.pricewatch.entity.Product;
import com.example.pricewatch.notification.NotificationService;
import com.example.pricewatch.price.PriceProvider;
import com.example.pricewatch.repository.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceUpdateService {

    private final ProductRepository productRepository;
    private final PriceProvider priceProvider;
    private final ProductPriceUpdateTransactionService productPriceUpdateTransactionService;
    private final NotificationService notificationService;

    public void updateAllActiveProducts() {
        for (Product product : productRepository.findByActiveTrue()) {
            try {
                BigDecimal newPrice = priceProvider.getPrice(product.getProductUrl());
                PriceUpdateResult result = productPriceUpdateTransactionService.updatePrice(product.getId(), newPrice);
                if (result.changed()) {
                    log.info("Price updated for product id={}: {} -> {}", product.getId(),
                            result.oldPrice(), result.newPrice());
                }
                if (result.targetReached()) {
                    notifyTargetPriceReached(result);
                }
            } catch (Exception exception) {
                log.warn("Unable to update price for product id={}: {}", product.getId(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    private void notifyTargetPriceReached(PriceUpdateResult result) {
        try {
            notificationService.notifyTargetPriceReached(
                    result.product(), result.oldPrice(), result.newPrice()
            );
        } catch (Exception exception) {
            log.error("Unable to send target price notification for product id={}: {}",
                    result.product().getId(), exception.getClass().getSimpleName());
        }
    }
}
