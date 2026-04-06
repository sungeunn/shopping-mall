package com.shoppingmall.domain.review.controller;

import com.shoppingmall.domain.review.dto.ReviewRequest;
import com.shoppingmall.domain.review.dto.ReviewResponse;
import com.shoppingmall.domain.review.service.ReviewService;
import com.shoppingmall.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "리뷰", description = "상품 리뷰 작성/조회")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성 - 배송 완료된 주문에 한해 작성 가능")
    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.createReview(userId, request));
    }

    @Operation(summary = "상품 리뷰 목록 조회")
    @GetMapping("/api/products/{productId}/reviews")
    public ApiResponse<Page<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(reviewService.getProductReviews(productId, pageable));
    }
}
