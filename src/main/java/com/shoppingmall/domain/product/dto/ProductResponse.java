package com.shoppingmall.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.shoppingmall.domain.product.entity.Product;

import java.time.LocalDateTime;

// Redis 캐싱 시 역직렬화를 위해 @class 타입 정보를 포함시킴
// Java record는 final이라 Jackson의 NON_FINAL 기본 타입 추론 대상에서 제외되므로 명시 필요
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
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
