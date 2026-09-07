package com.example.pricewatch.notification;

import com.example.pricewatch.entity.Product;
import java.math.BigDecimal;

public interface NotificationService {

    void notifyTargetPriceReached(Product product, BigDecimal oldPrice, BigDecimal newPrice);
}
