package com.alejomunoz.backend.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.alejomunoz.backend.infrastructure.persistence.entity.InventoryEntity;

import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long>  {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryEntity i WHERE i.product.id = :productId")
    Optional<InventoryEntity> findByProductIdWithLock(
        @Param("productId") Long productId
    );
}