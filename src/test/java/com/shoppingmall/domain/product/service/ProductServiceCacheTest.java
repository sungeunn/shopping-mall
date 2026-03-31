package com.shoppingmall.domain.product.service;

import com.shoppingmall.config.TestSecurityConfig;
import com.shoppingmall.domain.product.dto.ProductRequest;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ProductServiceCacheTest {

    // 테스트용 Redis 컨테이너 - @ServiceConnection이 host/port를 자동으로 주입해줌
    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .name("노트북").description("고성능 노트북").price(1_500_000).stock(10).category("전자기기").build();
        ReflectionTestUtils.setField(testProduct, "id", 1L);

        // 테스트 간 캐시 간섭 방지
        cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @Test
    @DisplayName("상품 단건 조회 - 두 번째 요청은 캐시 반환 (DB 미호출)")
    void getProduct_returnsCachedResult() {
        // given
        given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));

        // when - 동일한 ID로 두 번 조회
        productService.getProduct(1L);
        productService.getProduct(1L);

        // then - DB는 처음 한 번만 호출
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("상품 수정 후 캐시 무효화 - 다음 조회 시 DB 재조회")
    void updateProduct_evictsProductCache() {
        // given
        given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));
        ProductRequest updateRequest = new ProductRequest("노트북 Pro", "설명", 2_000_000, 5, "전자기기", null);

        // when
        productService.getProduct(1L);                   // 1회: DB 조회 + 캐시 저장
        productService.updateProduct(1L, updateRequest); // 2회: updateProduct 내부에서 findById 호출 + 캐시 무효화
        productService.getProduct(1L);                   // 3회: 캐시 없음 → DB 재조회

        // then - updateProduct 내부 조회 포함해서 총 3회
        verify(productRepository, times(3)).findById(1L);
    }

    @Test
    @DisplayName("상품 삭제 후 캐시 무효화 - 다음 조회 시 DB 재조회")
    void deleteProduct_evictsProductCache() {
        // given
        given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));

        // when
        productService.getProduct(1L);    // 1회: DB 조회 + 캐시 저장
        productService.deleteProduct(1L); // 2회: deleteProduct 내부에서 findById 호출 + 캐시 무효화
        productService.getProduct(1L);    // 3회: 캐시 없음 → DB 재조회

        // then - deleteProduct 내부 조회 포함해서 총 3회
        verify(productRepository, times(3)).findById(1L);
    }

    @Test
    @DisplayName("상품 목록 조회 - 같은 조건 두 번째 요청은 캐시 반환")
    void getProducts_returnsCachedResult() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);
        given(productRepository.findByStatus(eq(ProductStatus.ON_SALE), any(Pageable.class))).willReturn(page);

        // when - 동일 조건으로 두 번 조회
        productService.getProducts(null, null, pageable);
        productService.getProducts(null, null, pageable);

        // then - DB는 처음 한 번만 호출
        verify(productRepository, times(1)).findByStatus(eq(ProductStatus.ON_SALE), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 등록 후 목록 캐시 무효화 - 다음 조회 시 DB 재조회")
    void createProduct_evictsProductsCache() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);
        given(productRepository.findByStatus(eq(ProductStatus.ON_SALE), any(Pageable.class))).willReturn(page);
        given(productRepository.save(any(Product.class))).willReturn(testProduct);

        ProductRequest newProduct = new ProductRequest("마우스", "무선 마우스", 50_000, 20, "전자기기", null);

        // when
        productService.getProducts(null, null, pageable); // 1회 DB 조회 + 캐시 저장
        productService.createProduct(newProduct);          // 캐시 무효화
        productService.getProducts(null, null, pageable); // 캐시 없음 → 2회 DB 조회

        // then
        verify(productRepository, times(2)).findByStatus(eq(ProductStatus.ON_SALE), any(Pageable.class));
    }
}
