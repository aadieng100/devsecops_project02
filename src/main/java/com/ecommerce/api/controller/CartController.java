package com.ecommerce.api.controller;

import com.ecommerce.api.controller.dto.AddToCartRequest;
import com.ecommerce.api.controller.dto.CartResponse;
import com.ecommerce.api.model.Cart;
import com.ecommerce.api.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final CartService cartService;

    /**
     * GET /api/carts
     * Retrieve the active cart state for the user specified in X-User-Id header.
     * Calculates and returns item subtotals and total cart value.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        Cart cart = cartService.getCartForUser(userId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    /**
     * POST /api/carts/items
     * Add an item to the cart. If the product is already in the cart, increments the quantity.
     * Validates that the requested quantity does not exceed product stock.
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody AddToCartRequest request) {
        Cart cart = cartService.addItemToCart(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    /**
     * DELETE /api/carts/items/{productId}
     * Remove a specific product completely from the user's active cart.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItemFromCart(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long productId) {
        Cart cart = cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    /**
     * DELETE /api/carts/clear
     * Clear all items from the active cart.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<CartResponse> clearCart(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        Cart cart = cartService.clearCart(userId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }
}
