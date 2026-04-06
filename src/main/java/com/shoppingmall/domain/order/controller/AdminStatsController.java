package com.shoppingmall.domain.order.controller;

import com.shoppingmall.domain.order.dto.AdminStatsResponse;
import com.shoppingmall.domain.order.service.AdminStatsService;
import com.shoppingmall.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 통계", description = "주문/매출 통계")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "통계 조회 - 오늘 주문 수, 총 매출, 상품별 판매량")
    @GetMapping
    public ApiResponse<AdminStatsResponse> getStats() {
        return ApiResponse.ok(adminStatsService.getStats());
    }
}
