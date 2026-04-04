package com.shoppingmall.domain.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingmall.config.TestSecurityConfig;
import com.shoppingmall.domain.product.dto.ProductRequest;
import com.shoppingmall.domain.product.dto.ProductResponse;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.service.ProductService;
import com.shoppingmall.global.cache.RestPage;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import com.shoppingmall.domain.product.dto.ProductSearchCondition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ProductService productService;

    private ProductResponse createProductResponse() {
        return new ProductResponse(1L, "노트북", "고성능 노트북", 1_500_000, 10,
                "전자기기", null, ProductStatus.ON_SALE.name(), LocalDateTime.now());
    }

    @Test
    @DisplayName("상품 목록 조회 - 200 반환")
    void getProducts_success() throws Exception {
        given(productService.getProducts(any(), any(Pageable.class)))
                .willReturn(new RestPage<>(new PageImpl<>(List.of(createProductResponse()))));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("노트북"))
                .andExpect(jsonPath("$.data.content[0].price").value(1_500_000));
    }

    @Test
    @DisplayName("상품 목록 조회 - keyword + category 파라미터 전달 검증")
    void getProducts_withKeywordAndCategory() throws Exception {
        given(productService.getProducts(
                argThat(c -> "노트북".equals(c.keyword()) && "전자기기".equals(c.category())),
                any(Pageable.class)))
                .willReturn(new RestPage<>(new PageImpl<>(List.of(createProductResponse()))));

        mockMvc.perform(get("/api/products")
                        .param("keyword", "노트북")
                        .param("category", "전자기기"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("노트북"));
    }

    @Test
    @DisplayName("상품 목록 조회 - 가격 범위 파라미터 전달 검증")
    void getProducts_withPriceRange() throws Exception {
        given(productService.getProducts(
                argThat(c -> Integer.valueOf(1_000_000).equals(c.minPrice()) && Integer.valueOf(2_000_000).equals(c.maxPrice())),
                any(Pageable.class)))
                .willReturn(new RestPage<>(new PageImpl<>(List.of(createProductResponse()))));

        mockMvc.perform(get("/api/products")
                        .param("minPrice", "1000000")
                        .param("maxPrice", "2000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].price").value(1_500_000));
    }

    @Test
    @DisplayName("상품 상세 조회 - 200 반환")
    void getProduct_success() throws Exception {
        given(productService.getProduct(1L)).willReturn(createProductResponse());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("노트북"));
    }

    @Test
    @DisplayName("상품 상세 조회 - 존재하지 않는 상품 (404)")
    void getProduct_notFound() throws Exception {
        given(productService.getProduct(999L))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ErrorCode.PRODUCT_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("상품 등록 성공 - 관리자 (201 반환)")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createProduct_success() throws Exception {
        ProductRequest request = new ProductRequest("노트북", "고성능 노트북", 1_500_000, 10, "전자기기", null);
        given(productService.createProduct(any())).willReturn(createProductResponse());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("노트북"));
    }

    @Test
    @DisplayName("상품 등록 실패 - 필수값 누락 (400)")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createProduct_missingRequiredField() throws Exception {
        // name 누락
        ProductRequest request = new ProductRequest(null, "설명", 10000, 5, "전자기기", null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
