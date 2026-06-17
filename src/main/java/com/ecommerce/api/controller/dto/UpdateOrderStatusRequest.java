package com.ecommerce.api.controller.dto;

import jakarta.validation.constraints.NotNull;
import com.ecommerce.api.model.OrderStatus;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "status is required.")
    private OrderStatus status;
}
