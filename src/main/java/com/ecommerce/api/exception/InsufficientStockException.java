package com.ecommerce.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final String productName;
    private final int requested;
    private final int available;

    public InsufficientStockException(Long productId, String productName, int requested, int available) {
        super(String.format(
                "Insufficient stock for product '%s' (id=%d): requested %d but only %d available.",
                productName, productId, requested, available));
        this.productId = productId;
        this.productName = productName;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
