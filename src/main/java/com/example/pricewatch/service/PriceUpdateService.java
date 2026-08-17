package com.example.pricewatch.service;

import com.example.pricewatch.entity.Product;
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

    public void updateAllActiveProducts() {
        for (Product product : productRepository.findByActiveTrue()) {
            try {
                BigDecimal newPrice = priceProvider.getPrice(product.getProductUrl());
                if (product.getCurrentPrice().compareTo(newPrice) != 0) {
                    productPriceUpdateTransactionService.updatePrice(product.getId(), newPrice);
                }
            } catch (Exception exception) {
                log.warn("Unable to update price for product id={}: {}", product.getId(),
                        exception.getClass().getSimpleName());
            }
        }
    }
}
