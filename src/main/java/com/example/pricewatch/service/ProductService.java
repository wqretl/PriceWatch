package com.example.pricewatch.service;

import com.example.pricewatch.dto.ProductRequest;
import com.example.pricewatch.dto.ProductPageResponse;
import com.example.pricewatch.dto.ProductResponse;
import com.example.pricewatch.entity.Product;
import com.example.pricewatch.exception.InvalidProductFilterException;
import com.example.pricewatch.exception.ProductNotFoundException;
import com.example.pricewatch.mapper.ProductMapper;
import com.example.pricewatch.repository.ProductRepository;
import com.example.pricewatch.repository.specification.ProductSpecification;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final PriceHistoryService priceHistoryService;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        priceHistoryService.savePrice(savedProduct, savedProduct.getCurrentPrice());
        return productMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(
            String name,
            Boolean active,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer page,
            Integer size,
            String sort
    ) {
        validateFilters(minPrice, maxPrice, page, size);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Specification<Product> specification = ProductSpecification.nameContains(name)
                .and(ProductSpecification.hasActive(active))
                .and(ProductSpecification.currentPriceGreaterThanOrEqualTo(minPrice))
                .and(ProductSpecification.currentPriceLessThanOrEqualTo(maxPrice));
        Page<Product> products = productRepository.findAll(specification, pageable);

        return new ProductPageResponse(
                products.getContent().stream().map(productMapper::toResponse).toList(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.isFirst(),
                products.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findProductById(id));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductById(id);
        BigDecimal oldPrice = product.getCurrentPrice();
        productMapper.updateEntity(request, product);
        Product updatedProduct = productRepository.save(product);
        if (oldPrice.compareTo(updatedProduct.getCurrentPrice()) != 0) {
            priceHistoryService.savePrice(updatedProduct, updatedProduct.getCurrentPrice());
        }
        return productMapper.toResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(findProductById(id));
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private void validateFilters(BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size) {
        if (page < 0) {
            throw new InvalidProductFilterException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidProductFilterException("size must be between 1 and 100");
        }
        if (minPrice != null && minPrice.signum() < 0) {
            throw new InvalidProductFilterException("minPrice must be greater than or equal to 0");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new InvalidProductFilterException("maxPrice must be greater than or equal to 0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidProductFilterException("minPrice must not be greater than maxPrice");
        }
    }

    private Sort parseSort(String sortParameter) {
        String[] sortParts = sortParameter.split(",", -1);
        if (sortParts.length != 2 || sortParts[0].isBlank() || sortParts[1].isBlank()) {
            throw new InvalidProductFilterException("Sort must use the format field,direction");
        }

        String field = sortParts[0].trim();
        if (!List.of("id", "name", "currentPrice", "targetPrice", "active", "createdAt", "updatedAt")
                .contains(field)) {
            throw new InvalidProductFilterException("Unsupported sort field: " + field);
        }

        return switch (sortParts[1].trim().toLowerCase()) {
            case "asc" -> Sort.by(field).ascending();
            case "desc" -> Sort.by(field).descending();
            default -> throw new InvalidProductFilterException("Sort direction must be asc or desc");
        };
    }
}
