package com.alejomunoz.backend.application.mapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import com.alejomunoz.backend.application.dto.ProductResponseDTO;
import com.alejomunoz.backend.infrastructure.persistence.entity.ProductEntity;

class DtoMapperTest {

    @Test
    void toProductDTO_shouldMapAllFieldsIncludingImageUrl() {
        ProductEntity entity = ProductEntity.builder()
                .id(1L)
                .name("Laptop")
                .description("Gaming Laptop")
                .price(new BigDecimal("1999.99"))
                .imageUrl("https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=600&auto=format&fit=crop&q=80")
                .createdAt(OffsetDateTime.now())
                .build();

        ProductResponseDTO dto = DtoMapper.toProductDTO(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getName(), dto.getName());
        assertEquals(entity.getDescription(), dto.getDescription());
        assertEquals(entity.getPrice(), dto.getPrice());
        assertEquals(entity.getImageUrl(), dto.getImageUrl());
        assertEquals(entity.getCreatedAt(), dto.getCreatedAt());
    }

    @Test
    void toProductDTO_shouldSupportNullableImageUrl() {
        ProductEntity entity = ProductEntity.builder()
                .id(2L)
                .name("Monitor")
                .price(new BigDecimal("499.50"))
                .build();

        ProductResponseDTO dto = DtoMapper.toProductDTO(entity);

        assertEquals(2L, dto.getId());
        assertNull(dto.getImageUrl());
    }

    @Test
    void toProductDTO_shouldHandleNullEntity() {
        assertNull(DtoMapper.toProductDTO(null));
    }
}

