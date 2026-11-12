package com.alejomunoz.backend.application.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponseDTO {
    private Long id;
    private ProductResponseDTO product;
    private Integer quantity;

    @JsonProperty("priceAtOrder")
    private BigDecimal priceAtOrder;

    @JsonProperty("unitPrice")
    private BigDecimal unitPrice;
}