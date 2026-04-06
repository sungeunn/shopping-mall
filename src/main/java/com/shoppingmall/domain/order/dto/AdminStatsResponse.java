package com.shoppingmall.domain.order.dto;

import java.util.List;

public record AdminStatsResponse(
        long todayOrderCount,
        long totalRevenue,
        List<ProductSalesResponse> productSales
) {}
