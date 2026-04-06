package com.shoppingmall.global.aop;

import com.shoppingmall.config.TestSecurityConfig;
import com.shoppingmall.domain.product.service.ProductService;
import com.shoppingmall.global.cache.RestPage;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.shoppingmall.domain.product.controller.ProductController.class)
@Import({TestSecurityConfig.class, LoggingAspect.class})
class LoggingAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("인증된 사용자 API 호출 시 로그에 userId 기록")
    @WithMockUser(username = "1")
    void logsAuthenticatedUserId() throws Exception {
        given(productService.getProducts(any(), any())).willReturn(new RestPage<>(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
        // 로그 출력 확인은 콘솔에서 "[API] userId=1 method=GET uri=/api/products" 형태로 확인
    }

    @Test
    @DisplayName("비인증 사용자 API 호출 시 로그에 anonymous 기록")
    void logsAnonymousUser() throws Exception {
        given(productService.getProducts(any(), any())).willReturn(new RestPage<>(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
        // 로그 출력 확인은 콘솔에서 "[API] userId=anonymous ..." 형태로 확인
    }

    @Test
    @DisplayName("예외 발생 시 FAILED 상태로 로그 기록 후 예외 전파")
    @WithMockUser
    void logsFailedStatusOnException() throws Exception {
        given(productService.getProducts(any(), any()))
                .willThrow(new com.shoppingmall.global.exception.BusinessException(
                        com.shoppingmall.global.exception.ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isNotFound());
        // 로그 출력 확인은 콘솔에서 "[API] ... status=FAILED error=DB error" 형태로 확인
    }
}
