package com.alejomunoz.backend.infrastructure.scheduler;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alejomunoz.backend.domain.service.OrderService;
import com.alejomunoz.backend.infrastructure.persistence.entity.InventoryEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.OrderEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.OrderItemEntity;
import com.alejomunoz.backend.infrastructure.persistence.repository.InventoryRepository;
import com.alejomunoz.backend.infrastructure.persistence.repository.OrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Component 
@RequiredArgsConstructor 
public class OrderExpirationScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 15000)
    @Transactional 
    public void processExpiredOrder() {

        OffsetDateTime now = OffsetDateTime.now();
        List<OrderEntity> expiredOrders = orderRepository.findByStatusAndExpiresAtBefore("PENDING_PAYMENT", now);

        if(expiredOrders.isEmpty()) return ;

        log.info("Cron Job TTL: {} expired orders found to be processed, with stock to be released.", expiredOrders.size());

        for(OrderEntity order : expiredOrders) {
            try {
                orderService.expireOrder(order.getId());
            } catch(Exception ex) {
                log.error("Error while atomically expiring order #{}: {}", order.getId(), ex.getMessage(), ex);
            }         

        }
    }
}
