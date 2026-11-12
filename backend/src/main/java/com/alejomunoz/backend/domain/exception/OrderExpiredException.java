package com.alejomunoz.backend.domain.exception;

public class OrderExpiredException extends BusinessException {

    public OrderExpiredException(String message) {
        super(message);
    }
    
    public OrderExpiredException(Long orderId) {
        super(String.format("Order #%d has expired. The reserved stock has been released.", orderId));
    }
}
