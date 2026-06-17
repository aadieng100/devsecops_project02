package com.ecommerce.api.controller.dto;

import com.ecommerce.api.model.CartItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enriched cart response that includes computed subtotals per item and a total cart value.
 * This is a dedicated view model assembled by the controller layer.
 */
@Data
public class CartResponse {

    private Long cartId;
    private Long userId;
    private List<CartItemView> items;
    private BigDecimal cartTotal;

    @Data
    public static class CartItemView {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }

    public static CartResponse from(com.ecommerce.api.model.Cart cart) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setUserId(cart.getUser().getId());

        List<CartItemView> views = cart.getItems().stream().map(item -> {
            CartItemView view = new CartItemView();
            view.setCartItemId(item.getId());
            view.setProductId(item.getProduct().getId());
            view.setProductName(item.getProduct().getName());
            view.setUnitPrice(item.getProduct().getPrice());
            view.setQuantity(item.getQuantity());
            view.setSubtotal(item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity())));
            return view;
        }).collect(Collectors.toList());

        response.setItems(views);
        response.setCartTotal(views.stream()
                .map(CartItemView::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return response;
    }
}
