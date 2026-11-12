package com.alejomunoz.backend.domain.exception;

public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(String message) {
        super(message);
    }
    
    public OrderNotFoundException(Long orderId) {
        super(String.format("Order not found with ID: %d", orderId));
    }
}
