package com.shoppingmall.domain.order.controller;

import com.shoppingmall.domain.order.dto.OrderResponse;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.service.OrderService;
import com.shoppingmall.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 주문", description = "관리자 주문 상태 관리")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 상태 변경 (SHIPPED, COMPLETED)")
    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        return ApiResponse.ok(orderService.updateOrderStatus(orderId, status));
    }
}
