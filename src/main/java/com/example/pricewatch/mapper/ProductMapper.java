package com.example.pricewatch.mapper;

import com.example.pricewatch.dto.ProductRequest;
import com.example.pricewatch.dto.ProductResponse;
import com.example.pricewatch.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setProductUrl(request.productUrl());
        product.setCurrentPrice(request.currentPrice());
        product.setTargetPrice(request.targetPrice());
        product.setActive(request.active() == null ? true : request.active());
        return product;
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getProductUrl(),
                product.getCurrentPrice(),
                product.getTargetPrice(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public void updateEntity(ProductRequest request, Product product) {
        product.setName(request.name());
        product.setProductUrl(request.productUrl());
        product.setCurrentPrice(request.currentPrice());
        product.setTargetPrice(request.targetPrice());
        if (request.active() != null) {
            product.setActive(request.active());
        }
    }
}
