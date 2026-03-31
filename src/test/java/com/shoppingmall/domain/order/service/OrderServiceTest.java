package com.shoppingmall.domain.order.service;

import com.shoppingmall.domain.order.dto.OrderRequest;
import com.shoppingmall.domain.order.dto.OrderResponse;
import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.entity.UserRole;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("encoded_password")
                .name("테스터")
                .role(UserRole.ROLE_USER)
                .build();

        testProduct = Product.builder()
                .name("노트북")
                .description("고성능 노트북")
                .price(1_500_000)
                .stock(10)
                .category("전자기기")
                .build();
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_success() {
        // given
        OrderRequest request = new OrderRequest(
                List.of(new OrderRequest.OrderItemRequest(1L, 2)),
                "홍길동", "010-1234-5678", "서울시 강남구"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(productRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(testProduct));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        OrderResponse response = orderService.createOrder(1L, request);

        // then
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(response.totalPrice()).isEqualTo(3_000_000);
        assertThat(testProduct.getStock()).isEqualTo(8); // 재고 차감 확인
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_insufficientStock() {
        // given
        Product lowStockProduct = Product.builder()
                .name("노트북").description("설명").price(1_500_000).stock(1).category("전자기기").build();

        OrderRequest request = new OrderRequest(
                List.of(new OrderRequest.OrderItemRequest(1L, 5)),
                "홍길동", "010-1234-5678", "서울시 강남구"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(productRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(lowStockProduct));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_STOCK.getMessage());
    }

    @Test
    @DisplayName("주문 취소 - 다른 사용자 접근 불가")
    void cancelOrder_accessDenied() {
        // given
        User anotherUser = User.builder()
                .email("other@example.com").password("pw").name("다른사람").role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(anotherUser, "id", 2L); // 다른 유저 ID

        Order order = Order.builder()
                .user(anotherUser)
                .receiverName("다른사람").receiverPhone("010-0000-0000").address("주소")
                .build();

        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L)) // userId = 1L
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
    }
}
