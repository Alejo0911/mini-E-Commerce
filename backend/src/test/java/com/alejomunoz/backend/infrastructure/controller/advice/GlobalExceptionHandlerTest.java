package com.alejomunoz.backend.infrastructure.controller.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.alejomunoz.backend.application.dto.ErrorResponse;
import com.alejomunoz.backend.domain.exception.InvalidTokenException;
import com.alejomunoz.backend.domain.exception.OrderAlreadyProcessedException;
import com.alejomunoz.backend.domain.exception.OrderExpiredException;
import com.alejomunoz.backend.domain.exception.OrderNotFoundException;
import com.alejomunoz.backend.domain.exception.StockInsufficientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/orders/1");
    }

    @Test
    void shouldHandleStockInsufficientException() {
        StockInsufficientException ex = new StockInsufficientException("Stock insuficiente para el producto");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleStockInsufficient(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("Stock insuficiente para el producto", response.getBody().getMessage());
        assertEquals("/api/orders/1", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void shouldHandleOrderNotFoundException() {
        OrderNotFoundException ex = new OrderNotFoundException(1L);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOrderNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("/api/orders/1", response.getBody().getPath());
    }

    @Test
    void shouldHandleInvalidTokenException() {
        InvalidTokenException ex = new InvalidTokenException("Token expirado");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidToken(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Token expirado", response.getBody().getMessage());
    }

    @Test
    void shouldHandleOrderExpiredException() {
        OrderExpiredException ex = new OrderExpiredException(1L);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOrderExpired(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
    }

    @Test
    void shouldHandleOrderAlreadyProcessedException() {
        OrderAlreadyProcessedException ex = new OrderAlreadyProcessedException(1L, "PAID");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOrderAlreadyProcessed(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
    }

    @Test
    void shouldHandleGeneralExceptionWithoutExposingInternalDetails() {
        Exception ex = new RuntimeException("Sensitive database stacktrace");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("Ha ocurrido un error interno en el servidor. Por favor, contacte al soporte.",
                response.getBody().getMessage());
    }
}

