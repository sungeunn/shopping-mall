package com.shoppingmall.domain.order.entity;

public enum OrderStatus {
    PENDING,    // 주문 대기
    CONFIRMED,  // 주문 확인
    SHIPPED,    // 배송 중
    COMPLETED,  // 배송 완료
    CANCELLED   // 주문 취소
}
