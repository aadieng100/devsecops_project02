package com.ecommerce.api.service.impl;

import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.InsufficientStockException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.model.*;
import com.ecommerce.api.repository.*;
import com.ecommerce.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * Executes the full checkout transaction:
     * 1. Resolves cart
     * 2. Validates stock for every line item
     * 3. Deducts stock atomically
     * 4. Creates Order + OrderItems with price snapshot
     * 5. Clears the cart
     *
     * The entire operation is wrapped in a single @Transactional boundary so that
     * any failure (e.g., insufficient stock halfway through) causes a complete rollback.
     */
    @Override
    @Transactional
    public Order checkout(Long userId) {
        User user = resolveUser(userId);

        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout with an empty cart.");
        }

        // ---------- Phase 1: Stock validation pass (fail-fast before any mutation) ----------
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new InsufficientStockException(
                        product.getId(),
                        product.getName(),
                        cartItem.getQuantity(),
                        product.getStockQuantity());
            }
        }

        // ---------- Phase 2: Stock deduction + build order items ----------
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int qty = cartItem.getQuantity();

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - qty);
            productRepository.save(product);

            // Snapshot price at purchase time
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(qty)
                    .priceAtPurchase(product.getPrice())
                    .build();
            orderItems.add(orderItem);
        }

        // ---------- Phase 3: Persist Order ----------
        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .build();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // ---------- Phase 4: Clear the cart ----------
        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }

    @Override
    public List<Order> getOrdersForUser(Long userId) {
        User user = resolveUser(userId);
        return orderRepository.findByUserWithItemsOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------
    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
