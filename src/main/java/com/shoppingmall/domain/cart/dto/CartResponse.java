package com.shoppingmall.domain.cart.dto;

import com.shoppingmall.domain.cart.entity.Cart;
import com.shoppingmall.domain.cart.entity.CartItem;

import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items,
        int totalPrice
) {
    public record CartItemResponse(
            Long productId,
            String productName,
            int price,
            int quantity,
            int subtotal,
            String imageUrl
    ) {
        public static CartItemResponse from(CartItem item) {
            return new CartItemResponse(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getPrice(),
                    item.getQuantity(),
                    item.getProduct().getPrice() * item.getQuantity(),
                    item.getProduct().getImageUrl()
            );
        }
    }

    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getCartItems().stream().map(CartItemResponse::from).toList(),
                cart.getTotalPrice()
        );
    }
}
