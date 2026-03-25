package com.shoppingmall.domain.cart.service;

import com.shoppingmall.domain.cart.dto.CartRequest;
import com.shoppingmall.domain.cart.dto.CartResponse;
import com.shoppingmall.domain.cart.entity.Cart;
import com.shoppingmall.domain.cart.entity.CartItem;
import com.shoppingmall.domain.cart.repository.CartRepository;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.BusinessException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    return Cart.create(user);
                });
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse addItem(Long userId, CartRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem newItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(request.quantity())
                .build();

        cart.addOrUpdateItem(newItem);
        cartRepository.save(cart);

        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long productId, int quantity) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND))
                .updateQuantity(quantity);

        return CartResponse.from(cart);
    }

    @Transactional
    public void removeItem(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
        cart.removeItem(productId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(Cart::clear);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            return cartRepository.save(Cart.create(user));
        });
    }
}
