package com.alejomunoz.backend.domain.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alejomunoz.backend.application.dto.ProductResponseDTO;
import com.alejomunoz.backend.application.mapper.DtoMapper;
import com.alejomunoz.backend.infrastructure.persistence.entity.ProductEntity;
import com.alejomunoz.backend.infrastructure.persistence.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@RequiredArgsConstructor 
public class ProductService {

    private final ProductRepository productRepository;


    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        List<ProductEntity> products = productRepository.findAllWithInventory();

        return products.stream()
            .map(DtoMapper::toProductDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        ProductEntity product = productRepository.findByIdWithInventory(id)
        .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));

        return DtoMapper.toProductDTO(product);
    }
    
}
