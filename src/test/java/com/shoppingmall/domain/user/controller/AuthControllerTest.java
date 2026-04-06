package com.shoppingmall.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingmall.config.TestSecurityConfig;
import com.shoppingmall.domain.user.dto.LoginRequest;
import com.shoppingmall.domain.user.dto.SignupRequest;
import com.shoppingmall.domain.user.dto.TokenResponse;
import com.shoppingmall.domain.user.service.AuthService;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AuthService authService;

    @Test
    @DisplayName("회원가입 성공 - 201 반환")
    void signup_success() throws Exception {
        SignupRequest request = new SignupRequest("test@test.com", "Test1234!@", "홍길동", "010-1234-5678");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식 오류 (400)")
    void signup_invalidEmail() throws Exception {
        SignupRequest request = new SignupRequest("invalid-email", "Test1234!@", "홍길동", null);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복 (409)")
    void signup_emailDuplicated() throws Exception {
        SignupRequest request = new SignupRequest("test@test.com", "Test1234!@", "홍길동", null);
        doThrow(new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS))
                .when(authService).signup(any());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage()));
    }

    @Test
    @DisplayName("로그인 성공 - 토큰 반환")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest("test@test.com", "Test1234!@");
        TokenResponse tokenResponse = TokenResponse.of("accessToken", "refreshToken");
        given(authService.login(any())).willReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호 (401)")
    void login_invalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("test@test.com", "wrongPassword1!");
        given(authService.login(any())).willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("이메일 중복 체크 - 사용 가능")
    void checkEmail_available() throws Exception {
        given(authService.isEmailAvailable("new@test.com")).willReturn(true);

        mockMvc.perform(get("/api/auth/check-email")
                        .param("email", "new@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("이메일 중복 체크 - 이미 사용 중")
    void checkEmail_unavailable() throws Exception {
        given(authService.isEmailAvailable("test@test.com")).willReturn(false);

        mockMvc.perform(get("/api/auth/check-email")
                        .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("토큰 재발급 성공 - 새 accessToken/refreshToken 반환")
    void reissue_success() throws Exception {
        TokenResponse tokenResponse = TokenResponse.of("newAccessToken", "newRefreshToken");
        given(authService.reissue(anyString())).willReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/reissue")
                        .header("Refresh-Token", "validRefreshToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("newRefreshToken"));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰 (401)")
    void reissue_invalidToken() throws Exception {
        given(authService.reissue(anyString()))
                .willThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        mockMvc.perform(post("/api/auth/reissue")
                        .header("Refresh-Token", "invalidToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    @Test
    @DisplayName("로그아웃 성공 - 200 반환")
    @WithMockUser
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer validAccessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
    }

    @Test
    @DisplayName("로그인 실패 - 5회 초과 계정 잠금 (429)")
    void login_lockedAccount() throws Exception {
        LoginRequest request = new LoginRequest("test@test.com", "Test1234!@");
        given(authService.login(any())).willThrow(new BusinessException(ErrorCode.LOGIN_ATTEMPT_EXCEEDED));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ErrorCode.LOGIN_ATTEMPT_EXCEEDED.getMessage()));
    }
}
