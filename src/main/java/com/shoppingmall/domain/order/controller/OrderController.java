package com.shoppingmall.domain.order.controller;

import com.shoppingmall.domain.order.dto.OrderRequest;
import com.shoppingmall.domain.order.dto.OrderResponse;
import com.shoppingmall.domain.order.service.OrderService;
import com.shoppingmall.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "주문", description = "주문 생성/조회/취소")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createOrder(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OrderRequest request) {
        return ApiResponse.ok(orderService.createOrder(userId, request));
    }

    @Operation(summary = "내 주문 목록")
    @GetMapping
    public ApiResponse<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(orderService.getMyOrders(userId, pageable));
    }

    @Operation(summary = "주문 상세 조회")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getOrder(userId, orderId));
    }

    @Operation(summary = "주문 취소")
    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> cancelOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId) {
        orderService.cancelOrder(userId, orderId);
        return ApiResponse.ok("주문이 취소되었습니다.");
    }
}
