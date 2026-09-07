package com.example.pricewatch.service;

import com.example.pricewatch.entity.Product;
import com.example.pricewatch.exception.ProductNotFoundException;
import com.example.pricewatch.repository.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductPriceUpdateTransactionService {

    private final ProductRepository productRepository;
    private final PriceHistoryService priceHistoryService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PriceUpdateResult updatePrice(Long productId, BigDecimal newPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        BigDecimal oldPrice = product.getCurrentPrice();
        if (oldPrice.compareTo(newPrice) == 0) {
            return new PriceUpdateResult(product, oldPrice, newPrice, false, false);
        }

        product.setCurrentPrice(newPrice);
        Product updatedProduct = productRepository.save(product);
        priceHistoryService.savePrice(updatedProduct, newPrice);
        boolean targetReached = oldPrice.compareTo(updatedProduct.getTargetPrice()) > 0
                && newPrice.compareTo(updatedProduct.getTargetPrice()) <= 0;
        return new PriceUpdateResult(updatedProduct, oldPrice, newPrice, true, targetReached);
    }
}
