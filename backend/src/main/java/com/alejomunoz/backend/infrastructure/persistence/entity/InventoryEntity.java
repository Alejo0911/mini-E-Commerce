package com.alejomunoz.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "inventories")
@AllArgsConstructor 
@NoArgsConstructor 
@Data 
@Builder 
public class InventoryEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private ProductEntity product;

    @Column(name = "stock_available", nullable = false)
    private Integer stockAvailable;

    @Column(name = "stock_reserved", nullable = false)
    @Builder.Default
    private Integer stockReserved = 0;

    @Version 
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
