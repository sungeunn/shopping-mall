package com.shoppingmall.domain.order.dto;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String status,
        int totalPrice,
        String receiverName,
        String receiverPhone,
        String address,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public record OrderItemResponse(
            Long productId,
            String productName,
            int orderPrice,
            int quantity,
            int totalPrice
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getOrderPrice(),
                    item.getQuantity(),
                    item.getTotalPrice()
            );
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalPrice(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getAddress(),
                order.getOrderItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt()
        );
    }
}
