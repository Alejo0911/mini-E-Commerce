package com.alejomunoz.backend.infrastructure.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alejomunoz.backend.application.dto.CreateOrderRequestDTO;
import com.alejomunoz.backend.domain.service.OrderService;
import com.alejomunoz.backend.infrastructure.persistence.entity.OrderEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequestMapping("/api/orders")
@RequiredArgsConstructor 
public class OrderController {

    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderEntity> createOrder(@Valid @RequestBody CreateOrderRequestDTO requestDTO) {
        OrderEntity createdOrder = orderService.createOrderWithReservation(requestDTO);

        return  ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<OrderEntity> payOrder(@PathVariable Long id) {
        OrderEntity paidOrder = orderService.payOrder(id);

        return ResponseEntity.ok(paidOrder);
    }
}
