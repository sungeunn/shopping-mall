package com.shoppingmall.domain.user.dto;

import com.shoppingmall.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phone,
        String role,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
