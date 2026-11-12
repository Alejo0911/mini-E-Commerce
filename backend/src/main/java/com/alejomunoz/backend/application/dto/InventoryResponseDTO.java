package com.alejomunoz.backend.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder 
@AllArgsConstructor 
@NoArgsConstructor 
public class InventoryResponseDTO {
    private Long id;
    private Long productId;
    private Integer stockAvailable;
    private Integer stockReserved;
    private Long version;
    
}
