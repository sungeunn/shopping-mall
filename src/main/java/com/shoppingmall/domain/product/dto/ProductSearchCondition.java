package com.shoppingmall.domain.product.dto;

/**
 * 상품 동적 검색 조건
 * - 모든 필드가 null이면 전체 조회
 * - 조합 가능: keyword + category + minPrice + maxPrice
 */
public record ProductSearchCondition(
        String keyword,
        String category,
        Integer minPrice,
        Integer maxPrice
) {}
