package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.product.dto.ProductRequest;
import com.shoppingmall.domain.product.dto.ProductResponse;
import com.shoppingmall.domain.product.dto.ProductSearchCondition;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.global.cache.RestPage;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // 검색 조건 조합을 캐시 키로 사용
    // 예: "all_all_0_0_20", "전자기기_노트북_10000_500000_0_20"
    @Cacheable(
            value = "products",
            key = "(#condition.keyword() ?: 'all') + '_' + (#condition.category() ?: 'all') + '_' + (#condition.minPrice() ?: 0) + '_' + (#condition.maxPrice() ?: 0) + '_' + #pageable.pageNumber + '_' + #pageable.pageSize"
    )
    public RestPage<ProductResponse> getProducts(ProductSearchCondition condition, Pageable pageable) {
        return new RestPage<>(productRepository.search(condition, pageable).map(ProductResponse::from));
    }

    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    // 새 상품 등록 시 목록 캐시 전체 무효화
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .category(request.category())
                .imageUrl(request.imageUrl())
                .build();

        return ProductResponse.from(productRepository.save(product));
    }

    // 상품 수정 시 목록 캐시 전체 + 해당 상품 캐시 무효화
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.update(request.name(), request.description(), request.price(),
                request.category(), request.imageUrl());

        return ProductResponse.from(product);
    }

    // 상품 삭제(숨김) 시 목록 캐시 전체 + 해당 상품 캐시 무효화
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.changeStatus(ProductStatus.HIDDEN);
    }
}
