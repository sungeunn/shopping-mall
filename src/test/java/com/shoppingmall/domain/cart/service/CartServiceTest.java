package com.shoppingmall.domain.cart.service;

import com.shoppingmall.domain.cart.dto.CartRequest;
import com.shoppingmall.domain.cart.dto.CartResponse;
import com.shoppingmall.domain.cart.entity.Cart;
import com.shoppingmall.domain.cart.entity.CartItem;
import com.shoppingmall.domain.cart.repository.CartRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @InjectMocks
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .name("홍길동")
                .role(UserRole.ROLE_USER)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testProduct = Product.builder()
                .name("노트북")
                .description("고성능 노트북")
                .price(1_500_000)
                .stock(10)
                .category("전자기기")
                .build();
        ReflectionTestUtils.setField(testProduct, "id", 1L);

        testCart = Cart.create(testUser);
    }

    @Test
    @DisplayName("장바구니 상품 추가 성공")
    void addItem_success() {
        // given
        CartRequest request = new CartRequest(1L, 2);
        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
        given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));
        given(cartRepository.save(any(Cart.class))).willReturn(testCart);

        // when
        CartResponse response = cartService.addItem(1L, request);

        // then
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.totalPrice()).isEqualTo(3_000_000);
    }

    @Test
    @DisplayName("장바구니 상품 추가 - 이미 담긴 상품은 수량 합산")
    void addItem_existingProduct_quantityMerged() {
        // given
        CartItem existingItem = CartItem.builder()
                .cart(testCart).product(testProduct).quantity(1).build();
        testCart.addOrUpdateItem(existingItem);

        CartRequest request = new CartRequest(1L, 3);
        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
        given(productRepository.findById(1L)).willReturn(Optional.of(testProduct));
        given(cartRepository.save(any(Cart.class))).willReturn(testCart);

        // when
        CartResponse response = cartService.addItem(1L, request);

        // then
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(4); // 1 + 3
    }

    @Test
    @DisplayName("장바구니 상품 추가 실패 - 존재하지 않는 상품")
    void addItem_productNotFound() {
        // given
        CartRequest request = new CartRequest(999L, 1);
        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cartService.addItem(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("장바구니 상품 제거 성공")
    void removeItem_success() {
        // given
        CartItem item = CartItem.builder()
                .cart(testCart).product(testProduct).quantity(1).build();
        testCart.addOrUpdateItem(item);

        given(cartRepository.findByUserIdWithItems(1L)).willReturn(Optional.of(testCart));

        // when
        cartService.removeItem(1L, 1L); // productId = 1L

        // then
        assertThat(testCart.getCartItems()).isEmpty();
    }

    @Test
    @DisplayName("장바구니 비우기 성공")
    void clearCart_success() {
        // given
        CartItem item = CartItem.builder()
                .cart(testCart).product(testProduct).quantity(2).build();
        testCart.addOrUpdateItem(item);
        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(testCart));

        // when
        cartService.clearCart(1L);

        // then
        assertThat(testCart.getCartItems()).isEmpty();
    }
}
