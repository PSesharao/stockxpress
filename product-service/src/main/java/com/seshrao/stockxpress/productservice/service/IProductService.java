package com.seshrao.stockxpress.productservice.service;

import com.seshrao.stockxpress.productservice.dto.ProductRequest;
import com.seshrao.stockxpress.productservice.dto.ProductResponse;

import java.util.List;

/**
 * Service interface for Product operations.
 * Defines the contract for product management business logic.
 *
 * @author StockXpress Team
 */
public interface IProductService {
    
    /**
     * Create a new product.
     *
     * @param productRequest The product request containing product details
     * @return The created product response with generated ID
     * @throws com.seshrao.stockxpress.common.exception.ValidationException if validation fails
     * @throws com.seshrao.stockxpress.common.exception.BusinessException if business rules are violated
     */
    ProductResponse createProduct(ProductRequest productRequest);
    
    /**
     * Retrieve all products.
     *
     * @return List of all products
     */
    List<ProductResponse> getAllProducts();
    
    /**
     * Retrieve a product by ID.
     *
     * @param id The product ID
     * @return The product response
     * @throws com.seshrao.stockxpress.common.exception.ResourceNotFoundException if product not found
     */
    ProductResponse getProductById(String id);
    
    /**
     * Update an existing product.
     *
     * @param id The product ID
     * @param productRequest The updated product details
     * @return The updated product response
     * @throws com.seshrao.stockxpress.common.exception.ResourceNotFoundException if product not found
     * @throws com.seshrao.stockxpress.common.exception.ValidationException if validation fails
     */
    ProductResponse updateProduct(String id, ProductRequest productRequest);
    
    /**
     * Delete a product by ID.
     *
     * @param id The product ID
     * @throws com.seshrao.stockxpress.common.exception.ResourceNotFoundException if product not found
     */
    void deleteProduct(String id);
}
