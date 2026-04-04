package com.shoppingmall.domain.product.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shoppingmall.domain.product.dto.ProductSearchCondition;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.entity.QProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QProduct product = QProduct.product;

    @Override
    public Page<Product> search(ProductSearchCondition condition, Pageable pageable) {
        BooleanBuilder builder = buildCondition(condition);

        List<Product> content = queryFactory
                .selectFrom(product)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(product.createdAt.desc())
                .fetch();

        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private BooleanBuilder buildCondition(ProductSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        // 판매 중인 상품만 조회
        builder.and(product.status.eq(ProductStatus.ON_SALE));

        if (StringUtils.hasText(condition.keyword())) {
            builder.and(product.name.containsIgnoreCase(condition.keyword()));
        }
        if (StringUtils.hasText(condition.category())) {
            builder.and(product.category.containsIgnoreCase(condition.category()));
        }
        if (condition.minPrice() != null) {
            builder.and(product.price.goe(condition.minPrice()));
        }
        if (condition.maxPrice() != null) {
            builder.and(product.price.loe(condition.maxPrice()));
        }

        return builder;
    }
}
