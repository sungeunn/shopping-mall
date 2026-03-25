package com.shoppingmall.domain.cart.entity;

import com.shoppingmall.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    public static Cart create(User user) {
        Cart cart = new Cart();
        cart.user = user;
        return cart;
    }

    public void addOrUpdateItem(CartItem newItem) {
        Optional<CartItem> existing = cartItems.stream()
                .filter(item -> item.getProduct().getId().equals(newItem.getProduct().getId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().updateQuantity(existing.get().getQuantity() + newItem.getQuantity());
        } else {
            cartItems.add(newItem);
        }
    }

    public void removeItem(Long productId) {
        cartItems.removeIf(item -> item.getProduct().getId().equals(productId));
    }

    public void clear() {
        cartItems.clear();
    }

    public int getTotalPrice() {
        return cartItems.stream()
                .mapToInt(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }
}
