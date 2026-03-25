package com.shoppingmall.domain.product.dto;

import com.shoppingmall.domain.product.entity.Product;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        int price,
        int stock,
        String category,
        String imageUrl,
        String status,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getImageUrl(),
                product.getStatus().name(),
                product.getCreatedAt()
        );
    }
}
