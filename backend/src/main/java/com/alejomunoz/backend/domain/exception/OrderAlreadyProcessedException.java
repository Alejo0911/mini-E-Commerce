package com.alejomunoz.backend.domain.exception;

public class OrderAlreadyProcessedException extends BusinessException {

    public OrderAlreadyProcessedException(String message) {
        super(message);
    }

    public OrderAlreadyProcessedException(Long orderId, String currentStatus) {
        super(String.format("Order #%d has already been processed and is in the following state: %s", orderId, currentStatus));
    }
    
    
}
