package com.alejomunoz.backend.domain.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.alejomunoz.backend.application.dto.CreateOrderRequestDTO;
import com.alejomunoz.backend.infrastructure.persistence.entity.InventoryEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.OrderEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.OrderItemEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.ProductEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.UserEntity;
import com.alejomunoz.backend.infrastructure.persistence.repository.InventoryRepository;
import com.alejomunoz.backend.infrastructure.persistence.repository.OrderRepository;
import com.alejomunoz.backend.infrastructure.persistence.repository.ProductRepository;
import com.alejomunoz.backend.infrastructure.persistence.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    @Transactional
    public OrderEntity createOrderWithReservation(CreateOrderRequestDTO requestDTO) {
        // 1. Buscar al usuario que realiza la compra
        UserEntity user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + requestDTO.getUserId()));

        // 2. Inicializar la entidad de la orden (Estado inicial: PENDING_PAYMENT con TTL de 10 minutos)
        OrderEntity order = OrderEntity.builder()
                .user(user)
                .status("PENDING_PAYMENT")
                .expiresAt(OffsetDateTime.now().plusMinutes(10)) // TTL configurado
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Procesar cada ítem del pedido aplicando BLOQUEO PESIMISTA
        List<OrderItemEntity> orderItems = requestDTO.getItems().stream().map(itemReq -> {
            
            // AQUI OCURRE EL BLOQUEO PESIMISTA (SELECT FOR UPDATE)
            InventoryEntity inventory = inventoryRepository.findByProductIdWithLock(itemReq.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product id: " + itemReq.getProductId()));

            // Validar si hay stock disponible suficiente
            if (inventory.getStockAvailable() < itemReq.getQuantity()) {
                throw new IllegalStateException("Not enough stock available for product id: " + itemReq.getProductId());
            }

            // Descontar del stock disponible y sumar al stock reservado (TTL)
            inventory.setStockAvailable(inventory.getStockAvailable() - itemReq.getQuantity());
            inventory.setStockReserved(inventory.getStockReserved() + itemReq.getQuantity());
            inventoryRepository.save(inventory);

            // Obtener el producto para calcular precios
            ProductEntity product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + itemReq.getProductId()));

            // Construir el ítem de la orden
            return OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

        }).collect(Collectors.toList());

        // 4. Calcular el monto total de la orden
        for (OrderItemEntity item : orderItems) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        // 5. Persistir la orden (Gracias a CascadeType.ALL, los order_items se guardan solos)
        return orderRepository.save(order);
    }

    @Transactional 
    public OrderEntity payOrder(Long orderId) {
        // 1. Buscar la orden
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
        
        // 2. Validar que la orden esté pendiente de pago
        if(!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new IllegalStateException("Order cannot be paid because its status is: " + order.getStatus());
        }

        // 3. Validar que no haya expirado ya por tiempo
        if(OffsetDateTime.now().isAfter(order.getExpiresAt())) {
            order.setStatus("EXPIRED");
            orderRepository.save(order);

            throw new IllegalStateException("Order has already expired and cannot be paid.");
        }

        // 4. Cambiar estado a PAID
        order.setStatus("PAID");

        // 5. Consolidar el inventario (El stock reservado se descuenta definitivamente)
        for(OrderItemEntity item : order.getItems()) {
            Long productId = item.getProduct().getId();
            Integer quantity = item.getQuantity();

            InventoryEntity inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product id: " + productId));

            // Descontamos del stock reservado (ya fue vendido)
            inventory.setStockAvailable(Math.max(0, inventory.getStockReserved() - quantity));
            inventoryRepository.save(inventory);
        }

        return orderRepository.save(order);

    }
}
