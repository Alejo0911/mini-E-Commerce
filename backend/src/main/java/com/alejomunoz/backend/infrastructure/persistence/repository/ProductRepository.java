package com.alejomunoz.backend.infrastructure.persistence.repository;

import com.alejomunoz.backend.infrastructure.persistence.entity.ProductEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>{
    

}
