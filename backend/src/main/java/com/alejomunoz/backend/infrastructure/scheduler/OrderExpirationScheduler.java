package com.alejomunoz.backend.infrastructure.scheduler;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    private final InventoryRepository inventoryRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional 
    public void processExpiredOrder() {

        OffsetDateTime now = OffsetDateTime.now();
        List<OrderEntity> expiredOrders = orderRepository.findByStatusAndExpiresAtBefore("PENDING_PAYMENT", now);

        if(expiredOrders.isEmpty()) return ;

        log.info("Found {} expired orders to process and release stock.", expiredOrders.size());

        for(OrderEntity order : expiredOrders) {
            order.setStatus("EXPIRED");

            for(OrderItemEntity item : order.getItems()) {
                Long productId  = item.getProduct().getId();
                Integer quantity = item.getQuantity();

                InventoryEntity inventory = inventoryRepository.findByProductIdWithLock(productId).orElse(null);

                if(inventory != null) {
                    inventory.setStockAvailable(inventory.getStockAvailable() + quantity);
                    inventory.setStockReserved(Math.max(0, inventory.getStockReserved() - quantity));
                }

                inventoryRepository.save(inventory);
                log.info("Released {} units of product ID {} back to inventory from expired order ID {}", 
                            quantity, productId, order.getId());
            }

            orderRepository.save(order);
        }
    }
}
