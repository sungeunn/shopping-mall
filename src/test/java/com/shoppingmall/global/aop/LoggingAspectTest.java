package com.shoppingmall.global.aop;

import com.shoppingmall.config.TestSecurityConfig;
import com.shoppingmall.domain.product.service.ProductService;
import com.shoppingmall.global.cache.RestPage;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.filter.RequestIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.shoppingmall.domain.product.controller.ProductController.class)
@Import({TestSecurityConfig.class, LoggingAspect.class, RequestIdFilter.class})
class LoggingAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("API 응답 헤더에 X-Request-Id 포함")
    @WithMockUser
    void responseIncludesRequestId() throws Exception {
        given(productService.getProducts(any(), any())).willReturn(new RestPage<>(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    @DisplayName("인증된 사용자 API 호출 시 로그에 userId 기록")
    @WithMockUser(username = "1")
    void logsAuthenticatedUserId() throws Exception {
        given(productService.getProducts(any(), any())).willReturn(new RestPage<>(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
        // 콘솔 로그: [API] requestId=... userId=1 method=GET uri=/api/products time=...ms status=SUCCESS
    }

    @Test
    @DisplayName("비인증 사용자 API 호출 시 로그에 anonymous 기록")
    void logsAnonymousUser() throws Exception {
        given(productService.getProducts(any(), any())).willReturn(new RestPage<>(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
        // 콘솔 로그: [API] requestId=... userId=anonymous ...
    }

    @Test
    @DisplayName("예외 발생 시 FAILED 상태로 로그 기록 후 예외 전파")
    @WithMockUser
    void logsFailedStatusOnException() throws Exception {
        given(productService.getProducts(any(), any()))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isNotFound());
        // 콘솔 로그: [API] requestId=... status=FAILED error=상품을 찾을 수 없습니다.
    }
}
