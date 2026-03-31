package com.shoppingmall.domain.user.service;

import com.shoppingmall.domain.user.dto.LoginRequest;
import com.shoppingmall.domain.user.dto.SignupRequest;
import com.shoppingmall.domain.user.dto.TokenResponse;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.entity.UserRole;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .name("홍길동")
                .phone("010-1234-5678")
                .role(UserRole.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        SignupRequest request = new SignupRequest("test@test.com", "Test1234!@", "홍길동", "010-1234-5678");
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        // when
        authService.signup(request);

        // then
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_emailDuplicated() {
        // given
        SignupRequest request = new SignupRequest("test@test.com", "Test1234!@", "홍길동", null);
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("test@test.com", "Test1234!@");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(request.password(), testUser.getPassword())).willReturn(true);
        given(jwtProvider.createAccessToken(any(), any(), any())).willReturn("accessToken");
        given(jwtProvider.createRefreshToken(any())).willReturn("refreshToken");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_emailNotFound() {
        // given
        LoginRequest request = new LoginRequest("none@test.com", "Test1234!@");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_passwordMismatch() {
        // given
        LoginRequest request = new LoginRequest("test@test.com", "wrongPassword");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(request.password(), testUser.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("이메일 사용 가능 여부 - 사용 가능")
    void isEmailAvailable_true() {
        // given
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);

        // when & then
        assertThat(authService.isEmailAvailable("new@test.com")).isTrue();
    }

    @Test
    @DisplayName("이메일 사용 가능 여부 - 이미 사용 중")
    void isEmailAvailable_false() {
        // given
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        // when & then
        assertThat(authService.isEmailAvailable("test@test.com")).isFalse();
    }
}
