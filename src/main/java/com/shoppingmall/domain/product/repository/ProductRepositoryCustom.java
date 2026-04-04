package com.shoppingmall.domain.product.repository;

import com.shoppingmall.domain.product.dto.ProductSearchCondition;
import com.shoppingmall.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

    Page<Product> search(ProductSearchCondition condition, Pageable pageable);
}
