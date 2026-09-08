package com.seshrao.stockxpress.productservice.controller;

import com.seshrao.stockxpress.productservice.dto.ProductRequest;
import com.seshrao.stockxpress.productservice.dto.ProductResponse;
import com.seshrao.stockxpress.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * REST Controller for Product operations.
 * <p>
 * Endpoints:
 * - POST   /api/product              - Create a new product
 * - GET    /api/product              - Get all products
 * - GET    /api/product/{id}         - Get product by ID
 * - PUT    /api/product/{id}         - Update product
 * - DELETE /api/product/{id}         - Delete product
 * <p>
 * Features:
 * - Request validation with @Valid
 * - Proper HTTP status codes
 * - Structured logging with MDC
 * - Exception handling via GlobalExceptionHandler
 * - Caching support
 *
 * @author StockXpress Team
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    /**
     * Create a new product.
     *
     * @param productRequest Product data to create
     * @return ResponseEntity with created product and HTTP 201 CREATED
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest productRequest) {

        log.info("[ProductController] Creating product - name: {}, correlationId: {}",
                productRequest.getName(), MDC.get("correlationId"));

        ProductResponse response = productService.createProduct(productRequest);

        log.info("[ProductController] Product created successfully - productId: {}, correlationId: {}",
                response.getId(), MDC.get("correlationId"));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get all products.
     *
     * @return ResponseEntity with list of products and HTTP 200 OK
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        log.info("[ProductController] Fetching all products - correlationId: {}",
                MDC.get("correlationId"));

        List<ProductResponse> products = productService.getAllProducts();

        log.info("[ProductController] Retrieved {} products - correlationId: {}",
                products.size(), MDC.get("correlationId"));

        return ResponseEntity.ok(products);
    }

    /**
     * Get product by ID.
     *
     * @param id Product ID
     * @return ResponseEntity with product data and HTTP 200 OK
     * @throws com.seshrao.stockxpress.common.exception.ResourceNotFoundException if product not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {

        log.info("[ProductController] Fetching product by ID - productId: {}, correlationId: {}",
                id, MDC.get("correlationId"));

        ProductResponse product = productService.getProductById(id);

        log.info("[ProductController] Product retrieved successfully - productId: {}, name: {}, correlationId: {}",
                product.getId(), product.getName(), MDC.get("correlationId"));

        return ResponseEntity.ok(product);
    }

    /**
     * Update an existing product.
     *
     * @param id             Product ID to update
     * @param productRequest Updated product data
     * @return ResponseEntity with updated product and HTTP 200 OK
     * @throws com.seshrao.stockxpress.common.exception.ResourceNotFoundException if product not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest productRequest) {

        log.info("[ProductController] Updating product - productId: {}, name: {}, correlationId: {}",
                id, productRequest.getName(), MDC.get("correlationId"));

        ProductResponse response = productService.updateProduct(id, productRequest);

        log.info("[ProductController] Product updated successfully - productId: {}, correlationId: {}",
                response.getId(), MDC.get("correlationId"));

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a product.
     *
     * @param id Product ID to delete
     * @return ResponseEntity with HTTP 204 NO CONTENT
     * @throws com.seshrao.stockxpress.common.exception.ResourceNotFoundException if product not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {

        log.info("[ProductController] Deleting product - productId: {}, correlationId: {}",
                id, MDC.get("correlationId"));

        productService.deleteProduct(id);

        log.info("[ProductController] Product deleted successfully - productId: {}, correlationId: {}",
                id, MDC.get("correlationId"));

        return ResponseEntity.noContent().build();
    }
}
