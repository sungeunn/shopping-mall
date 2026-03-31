package com.shoppingmall.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "이름을 입력해주세요.")
        String name,

        String phone
) {}
