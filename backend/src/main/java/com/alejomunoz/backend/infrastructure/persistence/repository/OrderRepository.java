package com.alejomunoz.backend.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.alejomunoz.backend.infrastructure.persistence.entity.OrderEntity;

import io.lettuce.core.dynamic.annotation.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status AND o.expiresAt <= :now")
    List<OrderEntity> findByStatusAndExpiresAtBefore(
        @Param("status") String status,
        @Param("expiresAt") OffsetDateTime now
    );
}
