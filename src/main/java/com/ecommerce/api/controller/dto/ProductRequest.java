package com.ecommerce.api.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name must not be blank.")
    @Size(min = 2, max = 256, message = "Product name must be between 2 and 256 characters.")
    private String name;

    @NotBlank(message = "Product description must not be blank.")
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters.")
    private String description;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01.")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places.")
    private BigDecimal price;

    @NotBlank(message = "Category must not be blank.")
    @Size(max = 128, message = "Category must not exceed 128 characters.")
    private String category;

    @NotNull(message = "Stock quantity is required.")
    @Min(value = 0, message = "Stock quantity must be zero or more.")
    private Integer stockQuantity;
}
