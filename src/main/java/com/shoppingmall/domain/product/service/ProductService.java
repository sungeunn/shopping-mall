package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.product.dto.ProductRequest;
import com.shoppingmall.domain.product.dto.ProductResponse;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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

    public Page<ProductResponse> getProducts(String category, String keyword, Pageable pageable) {
        Page<Product> products;

        if (StringUtils.hasText(keyword)) {
            products = productRepository.findByStatusAndNameContaining(ProductStatus.ON_SALE, keyword, pageable);
        } else if (StringUtils.hasText(category)) {
            products = productRepository.findByStatusAndCategoryContaining(ProductStatus.ON_SALE, category, pageable);
        } else {
            products = productRepository.findByStatus(ProductStatus.ON_SALE, pageable);
        }

        return products.map(ProductResponse::from);
    }

    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    @Transactional
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

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.update(request.name(), request.description(), request.price(),
                request.category(), request.imageUrl());

        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.changeStatus(ProductStatus.HIDDEN);
    }
}
