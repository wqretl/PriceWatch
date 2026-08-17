package com.example.pricewatch.mapper;

import com.example.pricewatch.dto.PriceHistoryResponse;
import com.example.pricewatch.entity.PriceHistory;
import org.springframework.stereotype.Component;

@Component
public class PriceHistoryMapper {

    public PriceHistoryResponse toResponse(PriceHistory priceHistory) {
        return new PriceHistoryResponse(
                priceHistory.getId(),
                priceHistory.getPrice(),
                priceHistory.getCheckedAt()
        );
    }
}
