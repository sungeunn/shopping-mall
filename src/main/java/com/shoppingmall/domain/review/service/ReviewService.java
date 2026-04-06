package com.shoppingmall.domain.review.service;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.review.dto.ReviewRequest;
import com.shoppingmall.domain.review.dto.ReviewResponse;
import com.shoppingmall.domain.review.entity.Review;
import com.shoppingmall.domain.review.repository.ReviewRepository;
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
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        Order order = orderRepository.findByIdWithItems(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        boolean productInOrder = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(request.productId()));
        if (!productInOrder) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_IN_ORDER);
        }

        if (reviewRepository.existsByOrderIdAndProductIdAndUserId(
                request.orderId(), request.productId(), userId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Review review = Review.builder()
                .user(user)
                .product(order.getOrderItems().stream()
                        .filter(item -> item.getProduct().getId().equals(request.productId()))
                        .findFirst().orElseThrow().getProduct())
                .order(order)
                .rating(request.rating())
                .content(request.content())
                .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(ReviewResponse::from);
    }
}
