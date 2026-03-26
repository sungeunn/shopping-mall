package com.shoppingmall.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank
        @Size(min = 10, message = "비밀번호는 10자 이상이어야 합니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        String password,

        @NotBlank(message = "이름을 입력해주세요.")
        String name,

        String phone
) {}
