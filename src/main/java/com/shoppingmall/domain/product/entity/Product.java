package com.shoppingmall.domain.product.entity;

import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private String category;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Builder
    public Product(String name, String description, int price, int stock,
                   String category, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.imageUrl = imageUrl;
        this.status = ProductStatus.ON_SALE;
    }

    public void update(String name, String description, int price, String category, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    /**
     * 재고 차감 - 동시성 처리를 위해 비관적 락과 함께 사용
     */
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;
        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
        if (this.status == ProductStatus.SOLD_OUT && this.stock > 0) {
            this.status = ProductStatus.ON_SALE;
        }
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }
}
