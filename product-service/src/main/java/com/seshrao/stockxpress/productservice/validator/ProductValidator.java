package com.seshrao.stockxpress.productservice.validator;

import com.seshrao.stockxpress.common.exception.BusinessException;
import com.seshrao.stockxpress.common.exception.ValidationException;
import com.seshrao.stockxpress.productservice.dto.ProductRequest;
import com.seshrao.stockxpress.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator component for Product business validation.
 * Separates business validation logic from bean validation.
 *
 * @author StockXpress Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductValidator {
    
    private final ProductRepository productRepository;
    
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999.99");
    private static final String PRICE_ERROR_CODE = "PRODUCT_PRICE_INVALID";
    private static final String DUPLICATE_ERROR_CODE = "PRODUCT_DUPLICATE";
    
    /**
     * Validate product request for creation.
     *
     * @param productRequest The product request to validate
     * @throws ValidationException if validation fails
     * @throws BusinessException if business rules are violated
     */
    public void validateForCreate(ProductRequest productRequest) {
        log.debug("Validating product request for creation: name={}", productRequest.getName());
        
        List<String> errors = new ArrayList<>();
        
        // Business validation for price range
        if (productRequest.getPrice() != null && 
            productRequest.getPrice().compareTo(MAX_PRICE) > 0) {
            String errorMsg = String.format("Product price cannot exceed %s", MAX_PRICE);
            log.warn("Price validation failed: price={}, maxAllowed={}", 
                    productRequest.getPrice(), MAX_PRICE);
            throw new BusinessException(PRICE_ERROR_CODE, errorMsg);
        }
        
        // Check for duplicate product name (business rule)
        validateUniqueProductName(productRequest.getName(), null);
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Product validation failed", errors);
        }
        
        log.debug("Product validation successful for: {}", productRequest.getName());
    }
    
    /**
     * Validate product request for update.
     *
     * @param id The product ID being updated
     * @param productRequest The product request to validate
     * @throws ValidationException if validation fails
     * @throws BusinessException if business rules are violated
     */
    public void validateForUpdate(String id, ProductRequest productRequest) {
        log.debug("Validating product request for update: id={}, name={}", 
                id, productRequest.getName());
        
        // Business validation for price range
        if (productRequest.getPrice() != null && 
            productRequest.getPrice().compareTo(MAX_PRICE) > 0) {
            String errorMsg = String.format("Product price cannot exceed %s", MAX_PRICE);
            log.warn("Price validation failed: price={}, maxAllowed={}", 
                    productRequest.getPrice(), MAX_PRICE);
            throw new BusinessException(PRICE_ERROR_CODE, errorMsg);
        }
        
        // Check for duplicate product name (excluding current product)
        validateUniqueProductName(productRequest.getName(), id);
        
        log.debug("Product validation successful for update: id={}", id);
    }
    
    /**
     * Validate that product name is unique.
     *
     * @param name The product name to check
     * @param excludeId The product ID to exclude from check (for updates)
     * @throws BusinessException if duplicate name found
     */
    private void validateUniqueProductName(String name, String excludeId) {
        // This is a sample business rule - you can customize based on requirements
        // For now, we'll keep it simple and not enforce strict uniqueness
        // In real scenario, you might want to query the database
        
        log.debug("Validating unique product name: name={}, excludeId={}", name, excludeId);
        
        // Example: Check if product with same name exists
        // List<Product> existingProducts = productRepository.findByName(name);
        // if (!existingProducts.isEmpty()) {
        //     boolean isDuplicate = existingProducts.stream()
        //         .anyMatch(p -> !p.getId().equals(excludeId));
        //     if (isDuplicate) {
        //         throw new BusinessException(DUPLICATE_ERROR_CODE, 
        //             "Product with name '" + name + "' already exists");
        //     }
        // }
    }
    
    /**
     * Validate price value.
     *
     * @param price The price to validate
     * @throws BusinessException if price is invalid
     */
    public void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new BusinessException(PRICE_ERROR_CODE, "Price cannot be null");
        }
        
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(PRICE_ERROR_CODE, "Price must be greater than zero");
        }
        
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessException(PRICE_ERROR_CODE, 
                    String.format("Price cannot exceed %s", MAX_PRICE));
        }
    }
}
