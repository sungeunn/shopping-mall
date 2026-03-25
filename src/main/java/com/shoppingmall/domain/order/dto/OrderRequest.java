package com.shoppingmall.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "주문 상품을 선택해주세요.")
        List<OrderItemRequest> items,

        @NotBlank(message = "수령인 이름을 입력해주세요.")
        String receiverName,

        @NotBlank(message = "수령인 연락처를 입력해주세요.")
        String receiverPhone,

        @NotBlank(message = "배송지 주소를 입력해주세요.")
        String address
) {
    public record OrderItemRequest(
            @NotNull Long productId,
            @NotNull @Min(1) Integer quantity
    ) {}
}
