package com.ecommerce.api.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "Username must not be blank.")
    @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters.")
    private String username;

    @NotBlank(message = "Email must not be blank.")
    @Email(message = "Email must be a valid email address.")
    @Size(max = 256, message = "Email must not exceed 256 characters.")
    private String email;
}
