package com.shoppingmall.domain.order.entity;

import com.shoppingmall.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int orderPrice; // 주문 시점 가격 스냅샷

    @Builder
    public OrderItem(Order order, Product product, int quantity) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.orderPrice = product.getPrice();
        product.decreaseStock(quantity);
    }

    public int getTotalPrice() {
        return orderPrice * quantity;
    }

    public void restoreStock() {
        product.increaseStock(quantity);
    }
}
