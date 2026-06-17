package com.ecommerce.api.service;

import com.ecommerce.api.model.Cart;

public interface CartService {

    Cart getCartForUser(Long userId);

    Cart addItemToCart(Long userId, Long productId, int quantity);

    Cart removeItemFromCart(Long userId, Long productId);

    Cart clearCart(Long userId);
}
