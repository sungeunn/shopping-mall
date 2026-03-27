package com.shoppingmall.domain.cart.dto;

import jakarta.validation.constraints.NotBlank;

public record CartOrderRequest(
        @NotBlank(message = "수령인 이름을 입력해주세요.")
        String receiverName,

        @NotBlank(message = "수령인 연락처를 입력해주세요.")
        String receiverPhone,

        @NotBlank(message = "배송지 주소를 입력해주세요.")
        String address
) {}
