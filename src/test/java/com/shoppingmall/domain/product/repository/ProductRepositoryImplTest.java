package com.shoppingmall.domain.product.repository;

import com.shoppingmall.domain.product.dto.ProductSearchCondition;
import com.shoppingmall.domain.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(com.shoppingmall.config.QuerydslConfig.class)
class ProductRepositoryImplTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.save(product("노트북", "전자기기", 1_500_000));
        productRepository.save(product("무선 마우스", "전자기기", 50_000));
        productRepository.save(product("청바지", "의류", 80_000));
        productRepository.save(product("운동화", "신발", 120_000));
        productRepository.save(product("게이밍 노트북", "전자기기", 2_500_000));
    }

    private Product product(String name, String category, int price) {
        return Product.builder()
                .name(name).description("설명").price(price).stock(10).category(category).build();
    }

    @Test
    @DisplayName("조건 없음 → 전체 조회")
    void search_noCondition() {
        ProductSearchCondition condition = new ProductSearchCondition(null, null, null, null);
        Page<Product> result = productRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("keyword 검색 → 이름에 '노트북' 포함된 상품만 반환")
    void search_byKeyword() {
        ProductSearchCondition condition = new ProductSearchCondition("노트북", null, null, null);
        Page<Product> result = productRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("노트북", "게이밍 노트북");
    }

    @Test
    @DisplayName("category 검색 → '전자기기' 카테고리 상품만 반환")
    void search_byCategory() {
        ProductSearchCondition condition = new ProductSearchCondition(null, "전자기기", null, null);
        Page<Product> result = productRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("가격 범위 검색 → minPrice~maxPrice 사이 상품만 반환")
    void search_byPriceRange() {
        ProductSearchCondition condition = new ProductSearchCondition(null, null, 60_000, 200_000);
        Page<Product> result = productRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2); // 청바지(80,000), 운동화(120,000)
        assertThat(result.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("청바지", "운동화");
    }

    @Test
    @DisplayName("복합 조건 - keyword + category + 가격 범위")
    void search_combinedCondition() {
        // 전자기기 중 keyword='노트북', 가격 1,000,000 이상
        ProductSearchCondition condition = new ProductSearchCondition("노트북", "전자기기", 1_000_000, null);
        Page<Product> result = productRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2); // 노트북(1.5M), 게이밍 노트북(2.5M)
    }

    @Test
    @DisplayName("페이징 - size=2, page=0 → 2개만 반환, 전체 개수는 5")
    void search_paging() {
        ProductSearchCondition condition = new ProductSearchCondition(null, null, null, null);
        Page<Product> result = productRepository.search(condition, PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }
}
