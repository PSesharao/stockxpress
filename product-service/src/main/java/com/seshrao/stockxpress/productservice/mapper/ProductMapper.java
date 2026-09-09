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
     * Uses domain factory method to ensure business validation.
     * Generates SKU code from product name and assigns default category.
     *
     * @param productRequest The product request DTO
     * @return Product entity with business validation enforced
     */
    public Product toEntity(ProductRequest productRequest) {
        if (productRequest == null) {
            log.warn("ProductRequest is null, returning null entity");
            return null;
        }

        // Generate SKU code from product name (remove spaces, uppercase, limit to 50 chars)
        String skuCode = generateSkuCode(productRequest.getName());

        // Use domain factory method with validation
        return Product.create(
                productRequest.getName(),
                productRequest.getDescription(),
                productRequest.getPrice(),
                skuCode,
                "General" // Default category, could be enhanced to accept category in ProductRequest
        );
    }

    /**
     * Generates a SKU code from product name.
     * Format: Uppercase, underscores for spaces, alphanumeric only.
     *
     * @param productName The product name
     * @return Generated SKU code
     */
    private String generateSkuCode(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return "PRODUCT_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        // Convert to uppercase, replace spaces/special chars with underscores, limit length
        String sku = productName.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", ""); // Remove leading/trailing underscores

        // Limit to 50 characters
        if (sku.length() > 50) {
            sku = sku.substring(0, 50);
        }

        return sku;
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
     * Uses domain business logic methods to ensure validation.
     *
     * @param product        The existing product entity
     * @param productRequest The product request with updated data
     */
    public void updateEntity(Product product, ProductRequest productRequest) {
        if (product == null || productRequest == null) {
            log.warn("Product or ProductRequest is null, skipping update");
            return;
        }

        // Use domain methods for updates (enforces business validation)
        product.updateInfo(
                productRequest.getName(),
                productRequest.getDescription(),
                null // Keep existing category, or could extract from request
        );

        product.updatePrice(productRequest.getPrice());
    }
}
