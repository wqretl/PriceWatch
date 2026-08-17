package com.example.pricewatch.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductResponse> content,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages,
        Boolean first,
        Boolean last
) {
}
