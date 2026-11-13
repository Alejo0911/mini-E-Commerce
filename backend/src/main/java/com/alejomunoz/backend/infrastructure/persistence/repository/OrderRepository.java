package com.alejomunoz.backend.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.alejomunoz.backend.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status AND o.expiresAt <= :now")
    List<OrderEntity> findByStatusAndExpiresAtBefore(
        @Param("status") String status,
        @Param("now") OffsetDateTime now
    );

    @Query("SELECT o FROM OrderEntity o WHERE o.user.id = :userId AND o.status = 'PENDING_PAYMENT' AND o.expiresAt > :now ORDER BY o.id DESC")
    List<OrderEntity> findActivePendingOrdersByUser(
        @Param("userId") Long userId,
        @Param("now") OffsetDateTime now
    );
}
