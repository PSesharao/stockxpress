package com.seshrao.stockxpress.productservice.mapper;

import com.seshrao.stockxpress.productservice.dto.ProductRequest;
import com.seshrao.stockxpress.productservice.dto.ProductResponse;
import com.seshrao.stockxpress.productservice.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component for converting between Product entities and DTOs.
 * Follows Single Responsibility Principle by separating mapping logic.
 *
 * @author StockXpress Team
 */
@Component
@Slf4j
public class ProductMapper {
    
    /**
     * Convert ProductRequest DTO to Product entity.
     *
     * @param productRequest The product request DTO
     * @return Product entity
     */
    public Product toEntity(ProductRequest productRequest) {
        if (productRequest == null) {
            log.warn("ProductRequest is null, returning null entity");
            return null;
        }
        
        return Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .build();
    }
    
    /**
     * Convert Product entity to ProductResponse DTO.
     *
     * @param product The product entity
     * @return ProductResponse DTO
     */
    public ProductResponse toResponse(Product product) {
        if (product == null) {
            log.warn("Product entity is null, returning null response");
            return null;
        }
        
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();
    }
    
    /**
     * Convert list of Product entities to list of ProductResponse DTOs.
     *
     * @param products List of product entities
     * @return List of ProductResponse DTOs
     */
    public List<ProductResponse> toResponseList(List<Product> products) {
        if (products == null) {
            log.warn("Product list is null, returning empty list");
            return List.of();
        }
        
        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Update existing Product entity with ProductRequest data.
     *
     * @param product The existing product entity
     * @param productRequest The product request with updated data
     */
    public void updateEntity(Product product, ProductRequest productRequest) {
        if (product == null || productRequest == null) {
            log.warn("Product or ProductRequest is null, skipping update");
            return;
        }
        
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
    }
}
