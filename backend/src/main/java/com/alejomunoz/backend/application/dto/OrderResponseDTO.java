package com.alejomunoz.backend.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private Long userId;
    private String status;
    private BigDecimal totalAmount;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private List<OrderItemResponseDTO> items;
}
