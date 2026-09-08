package com.seshrao.stockxpress.productservice.service;

import com.seshrao.stockxpress.common.exception.BusinessException;
import com.seshrao.stockxpress.common.exception.ResourceNotFoundException;
import com.seshrao.stockxpress.productservice.dto.ProductRequest;
import com.seshrao.stockxpress.productservice.dto.ProductResponse;
import com.seshrao.stockxpress.productservice.mapper.ProductMapper;
import com.seshrao.stockxpress.productservice.model.Product;
import com.seshrao.stockxpress.productservice.repository.ProductRepository;
import com.seshrao.stockxpress.productservice.validator.ProductValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Product operations.
 * Implements IProductService interface following SOLID principles.
 * Uses caching for performance optimization.
 *
 * @author StockXpress Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductValidator productValidator;

    /**
     * {@inheritDoc}
     */
    @Override
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        log.info("[ProductService] Creating new product - name: {}, price: {}, correlationId: {}",
                productRequest.getName(), productRequest.getPrice(), MDC.get("correlationId"));

        try {
            // Business validation
            productValidator.validateForCreate(productRequest);
            log.debug("[ProductService] Product validation passed - name: {}", productRequest.getName());

            // Map DTO to Entity
            Product product = productMapper.toEntity(productRequest);

            // Save to database
            Product savedProduct = productRepository.save(product);

            // Map Entity to Response DTO
            ProductResponse response = productMapper.toResponse(savedProduct);

            log.info("[ProductService] Product created successfully - productId: {}, name: {}, price: {}, correlationId: {}",
                    savedProduct.getId(),
                    savedProduct.getName(),
                    savedProduct.getPrice(),
                    MDC.get("correlationId"));

            return response;

        } catch (DataAccessException e) {
            log.error("[ProductService] MongoDB error while creating product - name: {}, error: {}, correlationId: {}",
                    productRequest.getName(), e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to create product due to database error", e);
        } catch (BusinessException | ResourceNotFoundException e) {
            // Re-throw custom exceptions
            throw e;
        } catch (Exception e) {
            log.error("[ProductService] Unexpected error while creating product - name: {}, error: {}, correlationId: {}",
                    productRequest.getName(), e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to create product due to unexpected error", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = "products")
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        log.info("[ProductService] Fetching all products - correlationId: {}", MDC.get("correlationId"));

        try {
            List<Product> products = productRepository.findAll();

            log.info("[ProductService] Retrieved {} products from database - correlationId: {}",
                    products.size(), MDC.get("correlationId"));

            return productMapper.toResponseList(products);

        } catch (DataAccessException e) {
            log.error("[ProductService] MongoDB error while fetching products - error: {}, correlationId: {}",
                    e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to fetch products due to database error", e);
        } catch (Exception e) {
            log.error("[ProductService] Unexpected error while fetching products - error: {}, correlationId: {}",
                    e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to fetch products due to unexpected error", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        log.info("[ProductService] Fetching product by id: {}, correlationId: {}", id, MDC.get("correlationId"));

        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("[ProductService] Product not found with id: {}, correlationId: {}",
                                id, MDC.get("correlationId"));
                        return new ResourceNotFoundException("Product", "id", id);
                    });

            log.info("[ProductService] Product found - productId: {}, name: {}, correlationId: {}",
                    product.getId(), product.getName(), MDC.get("correlationId"));

            return productMapper.toResponse(product);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("[ProductService] MongoDB error while fetching product - id: {}, error: {}, correlationId: {}",
                    id, e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to fetch product due to database error", e);
        } catch (Exception e) {
            log.error("[ProductService] Unexpected error while fetching product - id: {}, error: {}, correlationId: {}",
                    id, e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to fetch product due to unexpected error", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest productRequest) {
        log.info("[ProductService] Updating product - id: {}, name: {}, correlationId: {}",
                id, productRequest.getName(), MDC.get("correlationId"));

        try {
            // Validate business rules
            productValidator.validateForUpdate(id, productRequest);
            log.debug("[ProductService] Product validation passed for update - id: {}, correlationId: {}",
                    id, MDC.get("correlationId"));

            // Find existing product
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("[ProductService] Product not found for update - id: {}, correlationId: {}",
                                id, MDC.get("correlationId"));
                        return new ResourceNotFoundException("Product", "id", id);
                    });

            // Update entity
            productMapper.updateEntity(product, productRequest);

            // Save updated product
            Product updatedProduct = productRepository.save(product);

            log.info("[ProductService] Product updated successfully - productId: {}, name: {}, price: {}, correlationId: {}",
                    updatedProduct.getId(),
                    updatedProduct.getName(),
                    updatedProduct.getPrice(),
                    MDC.get("correlationId"));

            return productMapper.toResponse(updatedProduct);

        } catch (ResourceNotFoundException | BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("[ProductService] MongoDB error while updating product - id: {}, error: {}, correlationId: {}",
                    id, e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to update product due to database error", e);
        } catch (Exception e) {
            log.error("[ProductService] Unexpected error while updating product - id: {}, error: {}, correlationId: {}",
                    id, e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to update product due to unexpected error", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void deleteProduct(String id) {
        log.info("[ProductService] Deleting product - id: {}, correlationId: {}", id, MDC.get("correlationId"));

        try {
            // Check if product exists
            if (!productRepository.existsById(id)) {
                log.warn("[ProductService] Product not found for deletion - id: {}, correlationId: {}",
                        id, MDC.get("correlationId"));
                throw new ResourceNotFoundException("Product", "id", id);
            }

            productRepository.deleteById(id);

            log.info("[ProductService] Product deleted successfully - productId: {}, correlationId: {}",
                    id, MDC.get("correlationId"));

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("[ProductService] MongoDB error while deleting product - id: {}, error: {}, correlationId: {}",
                    id, e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to delete product due to database error", e);
        } catch (Exception e) {
            log.error("[ProductService] Unexpected error while deleting product - id: {}, error: {}, correlationId: {}",
                    id, e.getMessage(), MDC.get("correlationId"), e);
            throw new BusinessException("Failed to delete product due to unexpected error", e);
        }
    }
}
