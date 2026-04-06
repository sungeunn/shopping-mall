package com.shoppingmall.domain.review.dto;

import com.shoppingmall.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long userId,
        String userName,
        int rating,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
