package com.ecommerce.api.controller;

import com.ecommerce.api.controller.dto.UpdateOrderStatusRequest;
import com.ecommerce.api.model.Order;
import com.ecommerce.api.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final OrderService orderService;

    /**
     * POST /api/orders/checkout
     * Execute the checkout transaction for the user specified in X-User-Id.
     * Validates stock, deducts quantities, creates the Order, and clears the cart.
     * The entire operation is atomic via @Transactional in the service layer.
     */
    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        Order order = orderService.checkout(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * GET /api/orders
     * Retrieve the complete historical order tracking list for the user in X-User-Id.
     */
    @GetMapping
    public ResponseEntity<List<Order>> getOrdersForUser(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(orderService.getOrdersForUser(userId));
    }

    /**
     * PATCH /api/orders/{id}/status
     * Update the status of a specific order.
     * Accepts a JSON body: { "status": "PAID" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order updated = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
