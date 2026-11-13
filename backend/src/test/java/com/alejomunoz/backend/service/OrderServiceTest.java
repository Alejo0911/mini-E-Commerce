package com.alejomunoz.backend.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alejomunoz.backend.application.dto.CreateOrderRequestDTO;
import com.alejomunoz.backend.application.dto.OrderItemRequestDTO;
import com.alejomunoz.backend.application.dto.OrderResponseDTO;
import com.alejomunoz.backend.domain.exception.OrderAlreadyProcessedException;
import com.alejomunoz.backend.domain.exception.OrderExpiredException;
import com.alejomunoz.backend.domain.exception.StockInsufficientException;
import com.alejomunoz.backend.domain.service.OrderService;
import com.alejomunoz.backend.infrastructure.persistence.entity.InventoryEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.OrderEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.OrderItemEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.ProductEntity;
import com.alejomunoz.backend.infrastructure.persistence.entity.UserEntity;
import com.alejomunoz.backend.infrastructure.persistence.repository.InventoryRepository;
import com.alejomunoz.backend.infrastructure.persistence.repository.OrderRepository;
import com.alejomunoz.backend.infrastructure.persistence.repository.ProductRepository;
import com.alejomunoz.backend.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private UserEntity user;
    private ProductEntity product;
    private InventoryEntity inventory;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hash")
                .role("ROLE_CUSTOMER")
                .build();

        product = ProductEntity.builder()
                .id(100L)
                .name("Laptop")
                .price(new BigDecimal("1000.00"))
                .build();

        inventory = InventoryEntity.builder()
                .id(1L)
                .product(product)
                .stockAvailable(10)
                .stockReserved(0)
                .version(0L)
                .build();
    }

    @Test
    void shouldCreateOrderAndReserveStockSuccessfully() {
        // Arrange
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(1L);
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(100L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findActivePendingOrdersByUser(eq(1L), any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(orderRepository.saveAndFlush(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity o = invocation.getArgument(0);
            o.setId(50L);
            return o;
        });

        // Act
        OrderResponseDTO response = orderService.createOrderWithReservation(request);

        // Assert
        assertNotNull(response);
        assertEquals("PENDING_PAYMENT", response.getStatus());
        assertEquals(new BigDecimal("2000.00"), response.getTotalAmount());
        assertEquals(8, inventory.getStockAvailable());
        assertEquals(2, inventory.getStockReserved());

        verify(inventoryRepository, times(1)).saveAndFlush(inventory);
        verify(orderRepository, times(1)).saveAndFlush(any(OrderEntity.class));
    }

    @Test
    void shouldReturnExistingActivePendingOrderWhenUserAlreadyHasOne() {
        // Arrange
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(1L);
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(100L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        OrderEntity existingOrder = OrderEntity.builder()
                .id(99L)
                .user(user)
                .status("PENDING_PAYMENT")
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .totalAmount(new BigDecimal("1000.00"))
                .items(Collections.emptyList())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findActivePendingOrdersByUser(eq(1L), any(OffsetDateTime.class)))
                .thenReturn(List.of(existingOrder));

        // Act
        OrderResponseDTO response = orderService.createOrderWithReservation(request);

        // Assert
        assertNotNull(response);
        assertEquals(99L, response.getId());
        assertEquals("PENDING_PAYMENT", response.getStatus());

        // Verify that inventory was NOT modified and no new order was saved
        verify(inventoryRepository, never()).findByProductIdWithLock(any());
        verify(inventoryRepository, never()).saveAndFlush(any());
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowStockInsufficientExceptionWhenStockIsLowerThanRequested() {
        // Arrange
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(1L);
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(100L);
        item.setQuantity(15); // Disponibles: 10
        request.setItems(List.of(item));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findActivePendingOrdersByUser(eq(1L), any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));

        // Act & Assert
        assertThrows(StockInsufficientException.class, () -> {
            orderService.createOrderWithReservation(request);
        });

        verify(inventoryRepository, never()).saveAndFlush(any(InventoryEntity.class));
        verify(orderRepository, never()).saveAndFlush(any(OrderEntity.class));
    }

    @Test
    void shouldPayOrderAndConsolidateStockSuccessfully() {
        // Arrange
        OrderItemEntity orderItem = OrderItemEntity.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(product.getPrice())
                .build();

        OrderEntity order = OrderEntity.builder()
                .id(50L)
                .user(user)
                .status("PENDING_PAYMENT")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .totalAmount(new BigDecimal("2000.00"))
                .items(List.of(orderItem))
                .build();

        inventory.setStockAvailable(8);
        inventory.setStockReserved(2);

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(orderRepository.saveAndFlush(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponseDTO response = orderService.payOrder(50L);

        // Assert
        assertEquals("PAID", response.getStatus());
        assertEquals(8, inventory.getStockAvailable());
        assertEquals(0, inventory.getStockReserved()); // Consolidado: reservado vuelve a 0
        verify(inventoryRepository, times(1)).saveAndFlush(inventory);
    }

    @Test
    void shouldCancelOrderAndRestoreStockToAvailable() {
        // Arrange
        OrderItemEntity orderItem = OrderItemEntity.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(product.getPrice())
                .build();

        OrderEntity order = OrderEntity.builder()
                .id(50L)
                .user(user)
                .status("PENDING_PAYMENT")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .totalAmount(new BigDecimal("2000.00"))
                .items(List.of(orderItem))
                .build();

        inventory.setStockAvailable(8);
        inventory.setStockReserved(2);

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(orderRepository.saveAndFlush(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponseDTO response = orderService.cancelOrder(50L);

        // Assert
        assertEquals("CANCELLED", response.getStatus());
        assertEquals(10, inventory.getStockAvailable()); // 8 + 2
        assertEquals(0, inventory.getStockReserved());   // 2 - 2
        verify(inventoryRepository, times(1)).saveAndFlush(inventory);
        verify(orderRepository, times(1)).saveAndFlush(order);
    }

    @Test
    void shouldGetActivePendingOrderByUserId() {
        OrderEntity activeOrder = OrderEntity.builder()
                .id(77L)
                .user(user)
                .status("PENDING_PAYMENT")
                .expiresAt(OffsetDateTime.now().plusMinutes(8))
                .totalAmount(new BigDecimal("500.00"))
                .items(Collections.emptyList())
                .build();

        when(orderRepository.findActivePendingOrdersByUser(eq(1L), any(OffsetDateTime.class)))
                .thenReturn(List.of(activeOrder));

        Optional<OrderResponseDTO> result = orderService.getActivePendingOrderByUserId(1L);

        assertTrue(result.isPresent());
        assertEquals(77L, result.get().getId());
        assertEquals("PENDING_PAYMENT", result.get().getStatus());
    }

    @Test
    void shouldExpireOrderAndReleaseReservedStockBackToAvailable() {
        // Arrange
        OrderItemEntity orderItem = OrderItemEntity.builder()
                .id(1L)
                .product(product)
                .quantity(3)
                .unitPrice(product.getPrice())
                .build();

        OrderEntity order = OrderEntity.builder()
                .id(50L)
                .user(user)
                .status("PENDING_PAYMENT")
                .expiresAt(OffsetDateTime.now().minusMinutes(5)) // Ya expirada
                .totalAmount(new BigDecimal("3000.00"))
                .items(List.of(orderItem))
                .build();

        inventory.setStockAvailable(7);
        inventory.setStockReserved(3);

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(orderRepository.saveAndFlush(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        orderService.expireOrder(50L);

        // Assert
        assertEquals("EXPIRED", order.getStatus());
        assertEquals(10, inventory.getStockAvailable()); // 7 + 3 restituido
        assertEquals(0, inventory.getStockReserved());  // 3 - 3 liberado
        verify(inventoryRepository, times(1)).saveAndFlush(inventory);
        verify(orderRepository, times(1)).saveAndFlush(order);
    }

    @Test
    void shouldThrowOrderAlreadyProcessedExceptionWhenPayingAlreadyPaidOrder() {
        // Arrange
        OrderEntity order = OrderEntity.builder()
                .id(50L)
                .user(user)
                .status("PAID")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .totalAmount(new BigDecimal("2000.00"))
                .build();

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(OrderAlreadyProcessedException.class, () -> orderService.payOrder(50L));
    }

    @Test
    void shouldExpireAndThrowOrderExpiredExceptionWhenPayingExpiredOrder() {
        // Arrange
        OrderItemEntity orderItem = OrderItemEntity.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(product.getPrice())
                .build();

        OrderEntity order = OrderEntity.builder()
                .id(50L)
                .user(user)
                .status("PENDING_PAYMENT")
                .expiresAt(OffsetDateTime.now().minusMinutes(2)) // Expiró hace 2 minutos
                .totalAmount(new BigDecimal("2000.00"))
                .items(List.of(orderItem))
                .build();

        inventory.setStockAvailable(8);
        inventory.setStockReserved(2);

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));

        // Act & Assert
        assertThrows(OrderExpiredException.class, () -> orderService.payOrder(50L));
        assertEquals("EXPIRED", order.getStatus());
        assertEquals(10, inventory.getStockAvailable());
        assertEquals(0, inventory.getStockReserved());
        verify(inventoryRepository, times(1)).saveAndFlush(inventory);
        verify(orderRepository, times(1)).saveAndFlush(order);
    }
}
