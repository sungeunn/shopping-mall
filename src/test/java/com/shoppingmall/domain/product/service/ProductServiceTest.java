package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.product.dto.ProductRequest;
import com.shoppingmall.domain.product.dto.ProductResponse;
import com.shoppingmall.domain.product.dto.ProductSearchCondition;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품 목록 조회 - 필터 없음")
    void getProducts_noFilter() {
        // given
        Product product = createProduct("노트북", 1_500_000, 10);
        PageRequest pageable = PageRequest.of(0, 20);
        ProductSearchCondition condition = new ProductSearchCondition(null, null, null, null);
        given(productRepository.search(condition, pageable))
                .willReturn(new PageImpl<>(List.of(product)));

        // when
        Page<ProductResponse> result = productService.getProducts(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("노트북");
    }

    @Test
    @DisplayName("상품 목록 조회 - keyword + category 동시 검색")
    void getProducts_keywordAndCategory() {
        // given
        Product product = createProduct("게이밍 노트북", 2_500_000, 5);
        PageRequest pageable = PageRequest.of(0, 20);
        ProductSearchCondition condition = new ProductSearchCondition("노트북", "전자기기", null, null);
        given(productRepository.search(condition, pageable))
                .willReturn(new PageImpl<>(List.of(product)));

        // when
        Page<ProductResponse> result = productService.getProducts(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("게이밍 노트북");
    }

    @Test
    @DisplayName("상품 목록 조회 - 가격 범위 검색")
    void getProducts_priceRange() {
        // given
        Product mouse = createProduct("무선 마우스", 50_000, 20);
        Product shoes = createProduct("운동화", 120_000, 15);
        PageRequest pageable = PageRequest.of(0, 20);
        ProductSearchCondition condition = new ProductSearchCondition(null, null, 30_000, 150_000);
        given(productRepository.search(condition, pageable))
                .willReturn(new PageImpl<>(List.of(mouse, shoes)));

        // when
        Page<ProductResponse> result = productService.getProducts(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(ProductResponse::price)
                .allSatisfy(price -> {
                    assertThat(price).isGreaterThanOrEqualTo(30_000);
                    assertThat(price).isLessThanOrEqualTo(150_000);
                });
    }

    @Test
    @DisplayName("상품 목록 조회 - keyword + category + 가격 범위 복합 검색")
    void getProducts_combined() {
        // given
        Product product = createProduct("노트북", 1_500_000, 10);
        PageRequest pageable = PageRequest.of(0, 20);
        ProductSearchCondition condition = new ProductSearchCondition("노트북", "전자기기", 1_000_000, 2_000_000);
        given(productRepository.search(condition, pageable))
                .willReturn(new PageImpl<>(List.of(product)));

        // when
        Page<ProductResponse> result = productService.getProducts(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("노트북");
    }

    @Test
    @DisplayName("상품 조회 - 존재하지 않는 ID")
    void getProduct_notFound() {
        // given
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("상품 등록 성공")
    void createProduct_success() {
        // given
        ProductRequest request = new ProductRequest("노트북", "고성능 노트북", 1_500_000, 10, "전자기기", null);
        Product product = createProduct("노트북", 1_500_000, 10);
        given(productRepository.save(any(Product.class))).willReturn(product);

        // when
        ProductResponse response = productService.createProduct(request);

        // then
        assertThat(response.name()).isEqualTo("노트북");
        assertThat(response.price()).isEqualTo(1_500_000);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("재고 차감 - 재고 부족 시 예외 발생")
    void decreaseStock_insufficientStock() {
        // given
        Product product = createProduct("노트북", 1_500_000, 2);

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_STOCK.getMessage());
    }

    @Test
    @DisplayName("재고 차감 - 재고 0이 되면 SOLD_OUT 상태로 변경")
    void decreaseStock_soldOut() {
        // given
        Product product = createProduct("노트북", 1_500_000, 3);

        // when
        product.decreaseStock(3);

        // then
        assertThat(product.getStock()).isZero();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    private Product createProduct(String name, int price, int stock) {
        return Product.builder()
                .name(name)
                .description("설명")
                .price(price)
                .stock(stock)
                .category("전자기기")
                .imageUrl(null)
                .build();
    }
}
