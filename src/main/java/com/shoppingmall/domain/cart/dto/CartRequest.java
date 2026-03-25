package com.shoppingmall.domain.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartRequest(
        @NotNull(message = "상품 ID를 입력해주세요.")
        Long productId,

        @NotNull @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        Integer quantity
) {}
