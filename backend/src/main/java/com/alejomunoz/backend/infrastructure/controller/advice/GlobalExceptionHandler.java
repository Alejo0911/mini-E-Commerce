package com.alejomunoz.backend.infrastructure.controller.advice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.alejomunoz.backend.application.dto.ErrorResponse;
import com.alejomunoz.backend.domain.exception.InvalidTokenException;
import com.alejomunoz.backend.domain.exception.OrderAlreadyProcessedException;
import com.alejomunoz.backend.domain.exception.OrderExpiredException;
import com.alejomunoz.backend.domain.exception.OrderNotFoundException;
import com.alejomunoz.backend.domain.exception.StockInsufficientException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockInsufficientException.class)
    public ResponseEntity<ErrorResponse> handleStockInsufficient(
            StockInsufficientException ex, HttpServletRequest request) {
        log.warn("Stock insuficiente en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(
            OrderNotFoundException ex, HttpServletRequest request) {
        log.warn("Orden no encontrada en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entidad no encontrada en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {
        log.warn("Acceso no autorizado en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(OrderExpiredException.class)
    public ResponseEntity<ErrorResponse> handleOrderExpired(
            OrderExpiredException ex, HttpServletRequest request) {
        log.warn("Orden expirada en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(OrderAlreadyProcessedException.class)
    public ResponseEntity<ErrorResponse> handleOrderAlreadyProcessed(
            OrderAlreadyProcessedException ex, HttpServletRequest request) {
        log.warn("Orden ya procesada en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Error de validación en {}: {}", request.getRequestURI(), errors);
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validación en los campos enviados",
                request.getRequestURI(), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Violación de restricciones en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Estado ilegal en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento inválido en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}: ", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error interno en el servidor. Por favor, contacte al soporte.",
                request.getRequestURI(), null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String message, String path, Map<String, String> validationErrors) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}

