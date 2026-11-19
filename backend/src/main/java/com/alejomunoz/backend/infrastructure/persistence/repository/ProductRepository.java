package com.alejomunoz.backend.infrastructure.persistence.repository;

import com.alejomunoz.backend.infrastructure.persistence.entity.ProductEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>{
    
    @Query("SELECT p FROM ProductEntity p LEFT JOIN FETCH p.inventory ORDER BY p.id ASC")
    List<ProductEntity> findAllWithInventory();

    @Query("SELECT p FROM ProductEntity p LEFT JOIN FETCH p.inventory WHERE p.id = :id")
    Optional<ProductEntity> findByIdWithInventory(@Param("id") Long id);
}
