package com.shoppingmall.domain.order.service;

import com.shoppingmall.domain.cart.dto.CartOrderRequest;
import com.shoppingmall.domain.cart.entity.Cart;
import com.shoppingmall.domain.cart.repository.CartRepository;
import com.shoppingmall.domain.order.dto.OrderRequest;
import com.shoppingmall.domain.order.dto.OrderResponse;
import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderItem;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Order order = Order.builder()
                .user(user)
                .receiverName(request.receiverName())
                .receiverPhone(request.receiverPhone())
                .address(request.address())
                .build();

        for (OrderRequest.OrderItemRequest itemReq : request.items()) {
            // 비관적 락으로 재고 차감 (동시 주문 시 데이터 정합성 보장)
            Product product = productRepository.findByIdWithPessimisticLock(itemReq.productId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.quantity())
                    .build();

            order.addOrderItem(orderItem);
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse createOrderFromCart(Long userId, CartOrderRequest request) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (cart.getCartItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        User user = cart.getUser();

        Order order = Order.builder()
                .user(user)
                .receiverName(request.receiverName())
                .receiverPhone(request.receiverPhone())
                .address(request.address())
                .build();

        for (var cartItem : cart.getCartItems()) {
            Product product = productRepository.findByIdWithPessimisticLock(cartItem.getProduct().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .build();

            order.addOrderItem(orderItem);
        }

        OrderResponse response = OrderResponse.from(orderRepository.save(order));
        cart.clear();
        return response;
    }

    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(OrderResponse::from);
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return OrderResponse.from(order);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        order.cancel();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        switch (status) {
            case SHIPPED -> order.ship();
            case COMPLETED -> order.complete();
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return OrderResponse.from(order);
    }
}
