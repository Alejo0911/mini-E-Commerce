package com.alejomunoz.backend.infrastructure.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alejomunoz.backend.application.dto.ProductResponseDTO;
import com.alejomunoz.backend.domain.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/products")
@RequiredArgsConstructor 
public class ProductController {

    private final ProductService productService;

    @GetMapping 
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        List<ProductResponseDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {
        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
}
