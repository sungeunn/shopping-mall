package com.shoppingmall.domain.order.dto;

public record ProductSalesResponse(
        Long productId,
        String productName,
        long totalQuantity,
        long totalRevenue
) {}
