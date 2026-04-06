package com.shoppingmall.domain.review.entity;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "product_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private int rating; // 1~5

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder
    public Review(User user, Product product, Order order, int rating, String content) {
        this.user = user;
        this.product = product;
        this.order = order;
        this.rating = rating;
        this.content = content;
    }
}
