package com.ecommerce.api.service.impl;

import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.InsufficientStockException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.model.Cart;
import com.ecommerce.api.model.CartItem;
import com.ecommerce.api.model.Product;
import com.ecommerce.api.model.User;
import com.ecommerce.api.repository.CartItemRepository;
import com.ecommerce.api.repository.CartRepository;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * Resolve active cart for user, creating it lazily if it does not yet exist.
     */
    @Override
    @Transactional
    public Cart getCartForUser(Long userId) {
        User user = resolveUser(userId);
        return cartRepository.findByUserWithItems(user)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().user(user).build()));
    }

    @Override
    @Transactional
    public Cart addItemToCart(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be a positive integer.");
        }

        User user = resolveUser(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));

        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);

        int newTotalQuantity = quantity;
        if (existingItem.isPresent()) {
            newTotalQuantity = existingItem.get().getQuantity() + quantity;
        }

        if (newTotalQuantity > product.getStockQuantity()) {
            throw new InsufficientStockException(
                    product.getId(),
                    product.getName(),
                    newTotalQuantity,
                    product.getStockQuantity());
        }

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(newTotalQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(newItem);
        }

        return cartRepository.findByUserWithItems(user).orElseThrow();
    }

    @Override
    @Transactional
    public Cart removeItemFromCart(Long userId, Long productId) {
        User user = resolveUser(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        cartItemRepository.deleteByCartAndProduct(cart, product);

        return cartRepository.findByUserWithItems(user).orElseThrow();
    }

    @Override
    @Transactional
    public Cart clearCart(Long userId) {
        User user = resolveUser(userId);
        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        cart.getItems().clear();
        return cartRepository.save(cart);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------
    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
