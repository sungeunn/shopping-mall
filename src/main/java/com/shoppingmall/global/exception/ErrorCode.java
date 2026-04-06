package com.shoppingmall.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    LOGIN_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도 횟수를 초과했습니다. 10분 후 다시 시도해주세요."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "요청 수량이 재고를 초과합니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "취소할 수 없는 주문 상태입니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 주문에 접근 권한이 없습니다."),
    PRICE_MISMATCH(HttpStatus.CONFLICT, "상품 가격이 변경되었습니다. 최신 가격을 확인 후 다시 주문해주세요."),

    // Review
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 리뷰를 작성한 주문입니다."),
    ORDER_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "배송 완료된 주문에만 리뷰를 작성할 수 있습니다."),
    PRODUCT_NOT_IN_ORDER(HttpStatus.BAD_REQUEST, "해당 주문에 포함되지 않은 상품입니다."),

    // Cart
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니에 해당 상품이 없습니다."),
    CART_EMPTY(HttpStatus.BAD_REQUEST, "장바구니가 비어있습니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
