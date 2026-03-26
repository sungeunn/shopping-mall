package com.shoppingmall.domain.user.service;

import com.shoppingmall.domain.user.dto.ChangePasswordRequest;
import com.shoppingmall.domain.user.dto.UpdateProfileRequest;
import com.shoppingmall.domain.user.dto.UserResponse;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.entity.UserRole;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        UserResponse response = userService.getMyInfo(1L);

        // then
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 존재하지 않는 유저")
    void getMyInfo_userNotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyInfo(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("프로필 수정 성공")
    void updateProfile_success() {
        // given
        UpdateProfileRequest request = new UpdateProfileRequest("김철수", "010-9999-8888");
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        UserResponse response = userService.updateProfile(1L, request);

        // then
        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.phone()).isEqualTo("010-9999-8888");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_success() {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("Test1234!@", "NewPass5678!@");
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("Test1234!@", "encodedPassword")).willReturn(true);
        given(passwordEncoder.encode("NewPass5678!@")).willReturn("newEncodedPassword");

        // when
        userService.changePassword(1L, request);

        // then
        verify(passwordEncoder).encode("NewPass5678!@");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void changePassword_wrongCurrentPassword() {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "NewPass5678!@");
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }
}
