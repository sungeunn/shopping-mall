package com.shoppingmall.domain.cart.controller;

import com.shoppingmall.domain.cart.dto.CartOrderRequest;
import com.shoppingmall.domain.cart.dto.CartRequest;
import com.shoppingmall.domain.cart.dto.CartResponse;
import com.shoppingmall.domain.cart.service.CartService;
import com.shoppingmall.domain.order.dto.OrderResponse;
import com.shoppingmall.domain.order.service.OrderService;
import com.shoppingmall.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장바구니", description = "장바구니 관리")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    @Operation(summary = "장바구니 조회")
    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(cartService.getCart(userId));
    }

    @Operation(summary = "상품 담기")
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CartRequest request) {
        return ApiResponse.ok(cartService.addItem(userId, request));
    }

    @Operation(summary = "수량 변경")
    @PatchMapping("/items/{productId}")
    public ApiResponse<CartResponse> updateItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @RequestParam int quantity) {
        return ApiResponse.ok(cartService.updateItem(userId, productId, quantity));
    }

    @Operation(summary = "상품 제거")
    @DeleteMapping("/items/{productId}")
    public ApiResponse<Void> removeItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId) {
        cartService.removeItem(userId, productId);
        return ApiResponse.ok("상품이 제거되었습니다.");
    }

    @Operation(summary = "장바구니 비우기")
    @DeleteMapping
    public ApiResponse<Void> clearCart(@AuthenticationPrincipal Long userId) {
        cartService.clearCart(userId);
        return ApiResponse.ok("장바구니가 비워졌습니다.");
    }

    @Operation(summary = "장바구니 상품 바로 주문하기")
    @PostMapping("/order")
    public ApiResponse<OrderResponse> orderFromCart(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CartOrderRequest request) {
        return ApiResponse.ok(orderService.createOrderFromCart(userId, request));
    }
}
