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
    public void updatePrice(Long productId, BigDecimal newPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (product.getCurrentPrice().compareTo(newPrice) == 0) {
            return;
        }

        product.setCurrentPrice(newPrice);
        Product updatedProduct = productRepository.save(product);
        priceHistoryService.savePrice(updatedProduct, newPrice);
    }
}
