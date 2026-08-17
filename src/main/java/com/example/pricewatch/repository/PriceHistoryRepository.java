package com.example.pricewatch.repository;

import com.example.pricewatch.entity.PriceHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByProductIdOrderByCheckedAtDesc(Long productId);
}
