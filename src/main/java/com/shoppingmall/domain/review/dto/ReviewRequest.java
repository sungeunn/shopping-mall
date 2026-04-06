package com.shoppingmall.domain.review.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotNull Long orderId,
        @NotNull Long productId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(max = 500) String content
) {}
