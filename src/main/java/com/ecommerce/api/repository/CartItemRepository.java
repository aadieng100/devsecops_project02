package com.ecommerce.api.repository;

import com.ecommerce.api.model.Cart;
import com.ecommerce.api.model.CartItem;
import com.ecommerce.api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    void deleteByCartAndProduct(Cart cart, Product product);
}
