package com.ecommerce.api.service;

import com.ecommerce.api.model.Order;
import com.ecommerce.api.model.OrderStatus;

import java.util.List;

public interface OrderService {

    Order checkout(Long userId);

    List<Order> getOrdersForUser(Long userId);

    Order updateOrderStatus(Long orderId, OrderStatus newStatus);
}
