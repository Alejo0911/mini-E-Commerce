package com.alejomunoz.backend.domain.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alejomunoz.backend.application.dto.CreateOrderRequestDTO;
import com.alejomunoz.backend.application.dto.OrderItemRequestDTO;
import com.alejomunoz.backend.application.dto.OrderResponseDTO;
import com.alejomunoz.backend.application.mapper.DtoMapper;
import com.alejomunoz.backend.domain.exception.OrderAlreadyProcessedException;
import com.alejomunoz.backend.domain.exception.OrderExpiredException;
import com.alejomunoz.backend.domain.exception.OrderNotFoundException;
import com.alejomunoz.backend.domain.exception.StockInsufficientException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@RequiredArgsConstructor 
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * Crea una orden y reserva temporalmente el stock del producto aplicando
     * bloqueo pesimista (Pessimistic Write) para evitar condiciones de carrera (Race Conditions).
     */
    @Transactional
    public OrderResponseDTO createOrderWithReservation(CreateOrderRequestDTO requestDTO) {
        log.info("Initiating order creation for user ID: {}", requestDTO.getUserId());

        // 1. Buscar al usuario que realiza la compra
        UserEntity user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + requestDTO.getUserId()));

        if(requestDTO.getItems() == null || requestDTO.getItems().isEmpty()) {
            throw new IllegalArgumentException("The order must contain at least one product.");
        }

        // 2. Inicializar la entidad de la orden (Estado inicial: PENDING_PAYMENT con TTL de 10 minutos)
        OrderEntity order = OrderEntity.builder()
                .user(user)
                .status("PENDING_PAYMENT")
                .expiresAt(OffsetDateTime.now().plusMinutes(10)) // TTL configurado
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItemEntity> orderItems = new ArrayList<>();

        // 3. Procesar cada ítem del pedido aplicando BLOQUEO PESIMISTA
        for(OrderItemRequestDTO itemReq : requestDTO.getItems()) {
            Long productId = itemReq.getProductId();
            Integer quantity = itemReq.getQuantity();

            if(quantity == null || quantity == 0) {
                throw new IllegalArgumentException("The requested quantity must be greater than zero.");
            }

            // SELECT FOR UPDATE sobre la fila de inventario
            InventoryEntity inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product ID: " + productId));

            // Validar stock disponible
            if(inventory.getStockAvailable() < quantity) {
                log.warn("Insufficient stock for product ID {}. Requested: {}, Available: {}",
                        productId, quantity, inventory.getStockAvailable());
                throw new StockInsufficientException(productId, quantity, inventory.getStockAvailable());
            }

            // Descontar del stock disponible y transferir a stock reservado (TTL)
            inventory.setStockAvailable(inventory.getStockAvailable() - quantity);
            inventory.setStockReserved(inventory.getStockReserved() + quantity);
            inventoryRepository.save(inventory);

            // Obtener producto para asignación de precio unitario
            ProductEntity product = productRepository.findByIdWithInventory(productId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productId));

            OrderItemEntity itemEntity = OrderItemEntity.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .build();
            
            orderItems.add(itemEntity);

            BigDecimal itemSubTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(itemSubTotal);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        OrderEntity savedOrder = orderRepository.save(order);
        log.info("Order #{} successfully created with a TTL until {}. Total amount: {}",
                savedOrder.getId(), savedOrder.getExpiresAt(), savedOrder.getTotalAmount());

        return DtoMapper.toOrderDTO(savedOrder);
    }

    /**
     * Procesa el pago de una orden pendiente, consolidando definitivamente la venta
     * y descontando el stock reservado.
     */
    @Transactional 
    public OrderResponseDTO payOrder(Long orderId) {
        log.info("Processing payment for order ID: {}", orderId);

        // 1. Buscar la orden
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // 2. Validar que la orden esté pendiente de pago
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            if ("PAID".equals(order.getStatus())) {
                throw new OrderAlreadyProcessedException(orderId, "PAID");
            }
            if ("EXPIRED".equals(order.getStatus())) {
                throw new OrderExpiredException(orderId);
            }
            throw new OrderAlreadyProcessedException(orderId, order.getStatus());
        }


        // 3. Validar que no haya expirado ya por tiempo
        if(OffsetDateTime.now().isAfter(order.getExpiresAt())) {
            log.warn("Payment attempt for order #{} expired. Initiating stock release.", orderId);
            revertStockAndExpire(order);
            throw new OrderExpiredException(orderId);
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

            log.info("Consolidated stock for product ID {}: {} units sold.", productId, quantity);
        }

        OrderEntity paidOrder = orderRepository.save(order);
        log.info("Order #{} successfully paid.", paidOrder.getId());

        return DtoMapper.toOrderDTO(paidOrder);
    }

    /**
     * Expira una orden de forma atómica en su propia transacción aislada (REQUIRES_NEW)
     * restituyendo el stock reservado al stock disponible.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);

        if(order == null) {
            log.warn("Order #{} not found for expiration.", orderId);
            return;
        }

        if(!"PENDING_PAYMENT".equals(order.getStatus())) {
            log.debug("Order #{} is no longer PENDING_PAYMENT (current status: {}). Expiration is skipped..",
                    orderId, order.getStatus());
            return;
        }

        revertStockAndExpire(order);
    }


    /**
     * Cancela una orden y restituye el stock al inventario disponible.
     */
    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new OrderAlreadyProcessedException(orderId, order.getStatus());
        }

        order.setStatus("CANCELLED");

        for (OrderItemEntity item : order.getItems()) {
            Long productId = item.getProduct().getId();
            Integer quantity = item.getQuantity();

            InventoryEntity inventory = inventoryRepository.findByProductIdWithLock(productId).orElse(null);
            if (inventory != null) {
                inventory.setStockAvailable(inventory.getStockAvailable() + quantity);
                inventory.setStockReserved(Math.max(0, inventory.getStockReserved() - quantity));
                inventoryRepository.save(inventory);
                log.info("Stock restored due to order cancellation #{}: {} units of product ID {}.",
                        orderId, quantity, productId);
            }
        }
        OrderEntity cancelledOrder = orderRepository.save(order);
        return DtoMapper.toOrderDTO(cancelledOrder);
    }

    /**
     * Obtiene una orden por su identificador único.
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return DtoMapper.toOrderDTO(order);
    }

    private void revertStockAndExpire(OrderEntity order) {
        order.setStatus("EXPIRED");

        for(OrderItemEntity item : order.getItems()) {
            Long productId = item.getProduct().getId();
            Integer quantity = item.getQuantity();

            InventoryEntity inventory = inventoryRepository.findByProductIdWithLock(productId).orElse(null);
            if (inventory != null) {
                inventory.setStockAvailable(inventory.getStockAvailable() + quantity);
                inventory.setStockReserved(Math.max(0, inventory.getStockReserved() - quantity));
                inventoryRepository.save(inventory);

                log.info("Stock reverted due to order expiration #{}: {} units returned to product ID {}.",
                        order.getId(), quantity, productId);
            }
        }

        orderRepository.save(order);
        log.info("Order #{} marked as EXPIRED and stock successfully released.", order.getId());
    }
}
