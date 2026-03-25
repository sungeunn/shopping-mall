package com.shoppingmall.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank(message = "상품명을 입력해주세요.")
        String name,

        String description,

        @NotNull @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        Integer price,

        @NotNull @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
        Integer stock,

        @NotBlank(message = "카테고리를 입력해주세요.")
        String category,

        String imageUrl
) {}
