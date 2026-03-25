package com.shoppingmall.domain.user.controller;

import com.shoppingmall.domain.user.dto.LoginRequest;
import com.shoppingmall.domain.user.dto.SignupRequest;
import com.shoppingmall.domain.user.dto.TokenResponse;
import com.shoppingmall.domain.user.service.AuthService;
import com.shoppingmall.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입/로그인/토큰 재발급")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ApiResponse.ok("회원가입이 완료되었습니다.");
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@RequestHeader("Refresh-Token") String refreshToken) {
        return ApiResponse.ok(authService.reissue(refreshToken));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ApiResponse.ok("로그아웃되었습니다.");
    }
}
