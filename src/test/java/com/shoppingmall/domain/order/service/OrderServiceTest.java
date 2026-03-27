package com.shoppingmall.domain.order.service;

import com.shoppingmall.domain.cart.dto.CartOrderRequest;
import com.shoppingmall.domain.cart.entity.Cart;
import com.shoppingmall.domain.cart.entity.CartItem;
import com.shoppingmall.domain.cart.repository.CartRepository;
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
}
