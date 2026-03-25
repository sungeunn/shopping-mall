package com.shoppingmall.domain.order.entity;

import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private int totalPrice;

    // 배송지 정보
    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String receiverPhone;

    @Column(nullable = false)
    private String address;

    @Builder
    public Order(User user, String receiverName, String receiverPhone, String address) {
        this.user = user;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.address = address;
        this.status = OrderStatus.PENDING;
        this.totalPrice = 0;
    }

    public void addOrderItem(OrderItem item) {
        this.orderItems.add(item);
        this.totalPrice += item.getTotalPrice();
    }

    public void cancel() {
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
        this.orderItems.forEach(OrderItem::restoreStock);
    }

    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
    }

    public void ship() {
        this.status = OrderStatus.SHIPPED;
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
