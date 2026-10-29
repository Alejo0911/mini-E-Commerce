package com.alejomunoz.backend.application.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data 
public class CreateOrderRequestDTO {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemRequestDTO> items;
}
