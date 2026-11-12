package com.alejomunoz.backend.domain.exception;

public class StockInsufficientException extends BusinessException {

    public StockInsufficientException(String message) {
        super(message);
    }
    
    public StockInsufficientException(Long productId, int requested, int available) {
        super(String.format("Insufficient stock for product ID %d. Requested: %d, Available: %d",
                productId, requested, available));
    }
}
