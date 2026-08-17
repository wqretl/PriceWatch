package com.example.pricewatch.service;

import com.example.pricewatch.dto.PriceHistoryResponse;
import com.example.pricewatch.entity.PriceHistory;
import com.example.pricewatch.entity.Product;
import com.example.pricewatch.exception.ProductNotFoundException;
import com.example.pricewatch.mapper.PriceHistoryMapper;
import com.example.pricewatch.repository.PriceHistoryRepository;
import com.example.pricewatch.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    private final PriceHistoryMapper priceHistoryMapper;

    @Transactional
    public void savePrice(Product product, BigDecimal price) {
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setProduct(product);
        priceHistory.setPrice(price);
        priceHistoryRepository.save(priceHistory);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getProductPriceHistory(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return priceHistoryRepository.findByProductIdOrderByCheckedAtDesc(productId).stream()
                .map(priceHistoryMapper::toResponse)
                .toList();
    }
}
