package com.alejomunoz.backend.domain.exception;

public class InvalidTokenException extends BusinessException{

    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException() {
        super("Invalid or expired authentication token");
    }
}
