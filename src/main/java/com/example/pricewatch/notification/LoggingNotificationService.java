package com.example.pricewatch.notification;

import com.example.pricewatch.entity.Product;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingNotificationService implements NotificationService {

    @Override
    public void notifyTargetPriceReached(Product product, BigDecimal oldPrice, BigDecimal newPrice) {
        log.info("Target price reached for product {}: old price = {}, new price = {}, target price = {}",
                product.getId(), oldPrice, newPrice, product.getTargetPrice());
    }
}
