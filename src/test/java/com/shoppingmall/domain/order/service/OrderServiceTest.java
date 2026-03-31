package com.shoppingmall.domain.order.service;

import com.shoppingmall.domain.cart.dto.CartOrderRequest;
import com.shoppingmall.domain.cart.entity.Cart;
import com.shoppingmall.domain.cart.entity.CartItem;
import com.shoppingmall.domain.cart.repository.CartRepository;
import com.shoppingmall.domain.order.dto.OrderRequest;
import com.shoppingmall.domain.order.dto.OrderResponse;
import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderItem;
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

import java.util.List;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private CartRepository cartRepository;

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
        ReflectionTestUtils.setField(testProduct, "id", 1L);
        ReflectionTestUtils.setField(testUser, "id", 1L);
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
    @DisplayName("주문 취소 성공 - PENDING 상태에서 취소 및 재고 복구")
    void cancelOrder_success() {
        // given
        Order order = Order.builder()
                .user(testUser).receiverName("홍길동").receiverPhone("010-1234-5678").address("서울시 강남구")
                .build();
        OrderItem orderItem = OrderItem.builder()
                .order(order).product(testProduct).quantity(3).build(); // stock: 10 → 7
        order.addOrderItem(orderItem);

        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(order));

        // when
        orderService.cancelOrder(1L, 1L);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(testProduct.getStock()).isEqualTo(10); // 재고 복구 확인 (7 → 10)
    }

    @Test
    @DisplayName("주문 취소 실패 - 취소 불가 상태 (SHIPPED)")
    void cancelOrder_cannotBeCancelled() {
        // given
        Order order = Order.builder()
                .user(testUser).receiverName("홍길동").receiverPhone("010-1234-5678").address("서울시 강남구")
                .build();
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED);

        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ORDER_CANNOT_BE_CANCELLED.getMessage());
    }

    @Test
    @DisplayName("주문 취소 - 다른 사용자 접근 불가")
    void cancelOrder_accessDenied() {
        // given
        User anotherUser = User.builder()
                .email("other@example.com").password("pw").name("다른사람").role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(anotherUser, "id", 2L);

        Order order = Order.builder()
                .user(anotherUser)
                .receiverName("다른사람").receiverPhone("010-0000-0000").address("주소")
                .build();

        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("장바구니 주문 생성 성공 - 주문 생성 후 장바구니 비워짐")
    void createOrderFromCart_success() {
        // given
        Cart cart = Cart.create(testUser);
        CartItem cartItem = CartItem.builder()
                .cart(cart).product(testProduct).quantity(2).build();
        cart.addOrUpdateItem(cartItem);

        CartOrderRequest request = new CartOrderRequest("홍길동", "010-1234-5678", "서울시 강남구");

        given(cartRepository.findByUserIdWithItems(1L)).willReturn(Optional.of(cart));
        given(productRepository.findByIdWithPessimisticLock(testProduct.getId()))
                .willReturn(Optional.of(testProduct));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        OrderResponse response = orderService.createOrderFromCart(1L, request);

        // then
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(response.totalPrice()).isEqualTo(3_000_000);
        assertThat(response.receiverName()).isEqualTo("홍길동");
        assertThat(response.items()).hasSize(1);
        assertThat(testProduct.getStock()).isEqualTo(8); // 재고 차감 확인
        assertThat(cart.getCartItems()).isEmpty(); // 장바구니 비워짐 확인
    }

    @Test
    @DisplayName("장바구니 주문 생성 실패 - 장바구니가 비어있음")
    void createOrderFromCart_emptyCart() {
        // given
        Cart emptyCart = Cart.create(testUser);
        CartOrderRequest request = new CartOrderRequest("홍길동", "010-1234-5678", "서울시 강남구");

        given(cartRepository.findByUserIdWithItems(1L)).willReturn(Optional.of(emptyCart));

        // when & then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.CART_EMPTY.getMessage());
    }

    @Test
    @DisplayName("장바구니 주문 생성 실패 - 재고 부족")
    void createOrderFromCart_insufficientStock() {
        // given
        Product lowStockProduct = Product.builder()
                .name("노트북").description("설명").price(1_500_000).stock(1).category("전자기기").build();

        Cart cart = Cart.create(testUser);
        CartItem cartItem = CartItem.builder()
                .cart(cart).product(lowStockProduct).quantity(5).build();
        cart.addOrUpdateItem(cartItem);

        CartOrderRequest request = new CartOrderRequest("홍길동", "010-1234-5678", "서울시 강남구");

        given(cartRepository.findByUserIdWithItems(1L)).willReturn(Optional.of(cart));
        given(productRepository.findByIdWithPessimisticLock(lowStockProduct.getId()))
                .willReturn(Optional.of(lowStockProduct));

        // when & then
        assertThatThrownBy(() -> orderService.createOrderFromCart(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_STOCK.getMessage());
    }

    @Test
    @DisplayName("장바구니 주문 생성 성공 - 여러 상품 주문")
    void createOrderFromCart_multipleItems() {
        // given
        Product anotherProduct = Product.builder()
                .name("마우스").description("무선 마우스").price(50_000).stock(5).category("전자기기").build();
        ReflectionTestUtils.setField(anotherProduct, "id", 2L);

        Cart cart = Cart.create(testUser);
        cart.addOrUpdateItem(CartItem.builder().cart(cart).product(testProduct).quantity(1).build());
        cart.addOrUpdateItem(CartItem.builder().cart(cart).product(anotherProduct).quantity(2).build());

        CartOrderRequest request = new CartOrderRequest("홍길동", "010-1234-5678", "서울시 강남구");

        given(cartRepository.findByUserIdWithItems(1L)).willReturn(Optional.of(cart));
        given(productRepository.findByIdWithPessimisticLock(testProduct.getId()))
                .willReturn(Optional.of(testProduct));
        given(productRepository.findByIdWithPessimisticLock(anotherProduct.getId()))
                .willReturn(Optional.of(anotherProduct));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        OrderResponse response = orderService.createOrderFromCart(1L, request);

        // then
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalPrice()).isEqualTo(1_600_000); // 1_500_000 + 100_000
        assertThat(cart.getCartItems()).isEmpty();
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 - 배송중(SHIPPED)")
    void updateOrderStatus_shipped() {
        // given
        Order order = Order.builder()
                .user(testUser).receiverName("홍길동").receiverPhone("010-1234-5678").address("서울시 강남구")
                .build();

        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(order));

        // when
        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.SHIPPED);

        // then
        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED.name());
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 - 배송완료(COMPLETED)")
    void updateOrderStatus_completed() {
        // given
        Order order = Order.builder()
                .user(testUser).receiverName("홍길동").receiverPhone("010-1234-5678").address("서울시 강남구")
                .build();

        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(order));

        // when
        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.COMPLETED);

        // then
        assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED.name());
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 실패 - 존재하지 않는 주문")
    void updateOrderStatus_orderNotFound() {
        // given
        given(orderRepository.findByIdWithItems(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.updateOrderStatus(999L, OrderStatus.SHIPPED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 실패 - 허용되지 않는 상태값")
    void updateOrderStatus_invalidStatus() {
        // given
        Order order = Order.builder()
                .user(testUser).receiverName("홍길동").receiverPhone("010-1234-5678").address("서울시 강남구")
                .build();

        given(orderRepository.findByIdWithItems(1L)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT.getMessage());
    }
}
