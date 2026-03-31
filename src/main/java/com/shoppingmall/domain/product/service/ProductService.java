package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.product.dto.ProductRequest;
import com.shoppingmall.domain.product.dto.ProductResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // category, keyword, 페이지 번호/크기를 조합한 키로 캐싱
    // 예: "all_all_0_20", "전자기기_all_0_20", "all_노트북_0_20"
    @Cacheable(
            value = "products",
            key = "(#category ?: 'all') + '_' + (#keyword ?: 'all') + '_' + #pageable.pageNumber + '_' + #pageable.pageSize"
    )
    public RestPage<ProductResponse> getProducts(String category, String keyword, Pageable pageable) {
        Page<Product> products;

        if (StringUtils.hasText(keyword)) {
            products = productRepository.findByStatusAndNameContaining(ProductStatus.ON_SALE, keyword, pageable);
        } else if (StringUtils.hasText(category)) {
            products = productRepository.findByStatusAndCategoryContaining(ProductStatus.ON_SALE, category, pageable);
        } else {
            products = productRepository.findByStatus(ProductStatus.ON_SALE, pageable);
        }

        return new RestPage<>(products.map(ProductResponse::from));
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
