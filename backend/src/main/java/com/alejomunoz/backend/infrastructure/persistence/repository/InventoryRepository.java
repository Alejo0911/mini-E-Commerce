package com.alejomunoz.backend.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.alejomunoz.backend.infrastructure.persistence.entity.InventoryEntity;

import io.lettuce.core.dynamic.annotation.Param;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long>  {

    @Query("SELECT i FROM InventoryEntity i WHERE i.product.id = :productId")
    Optional<InventoryEntity> findByProductIdWithLock(
        @Param("productId") Long productId
    );
}
