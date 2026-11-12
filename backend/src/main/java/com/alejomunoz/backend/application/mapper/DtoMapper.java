package com.alejomunoz.backend.application.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.alejomunoz.backend.application.dto.InventoryResponseDTO;
import com.alejomunoz.backend.application.dto.OrderItemResponseDTO;
import com.alejomunoz.backend.application.dto.OrderResponseDTO;
import com.alejomunoz.backend.application.dto.ProductResponseDTO;
import com.alejomunoz.backend.infrastructure.persistence.entity.InventoryEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.OrderEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.OrderItemEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.ProductEntity;

public class DtoMapper {
    private DtoMapper() {}

    public static InventoryResponseDTO toInventoryDTO(InventoryEntity inventory) {
        if(inventory == null) return  null;

        return InventoryResponseDTO.builder()
        .id(inventory.getId())
        .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null )
        .stockAvailable(inventory.getStockAvailable())
        .stockReserved(inventory.getStockReserved())
        .version(inventory.getVersion())
        .build();
    }

    public static ProductResponseDTO toProductDTO(ProductEntity product) {
        if(product == null) return  null;

        return ProductResponseDTO.builder()
        .id(product.getId())
        .name(product.getName())
        .description(product.getDescription())
        .price(product.getPrice())
        .createdAt(product.getCreatedAt())
        .inventory(toInventoryDTO(product.getInventory()))
        .build();
    }

    public static OrderItemResponseDTO toOrderItemDTO(OrderItemEntity item) {
        if (item == null) return null;

        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .product(toProductDTO(item.getProduct()))
                .quantity(item.getQuantity())
                .priceAtOrder(item.getUnitPrice())
                .unitPrice(item.getUnitPrice())
                .build();
    }

    public static OrderResponseDTO toOrderDTO(OrderEntity order) {
        if (order == null) return null;
        
        List<OrderItemResponseDTO> items = order.getItems() == null ? Collections.emptyList()
                : order.getItems().stream()
                        .map(DtoMapper::toOrderItemDTO)
                        .collect(Collectors.toList());

        return OrderResponseDTO.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .expiresAt(order.getExpiresAt())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }
}
