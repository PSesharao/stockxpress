package com.seshrao.stockxpress.common.exception;

import lombok.Getter;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 NOT FOUND response.
 * 
 * @author StockXpress Team
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Constructor with resource details
     * 
     * @param resourceName The name of the resource (e.g., "Product", "Order")
     * @param fieldName The field name used for search (e.g., "id", "skuCode")
     * @param fieldValue The value that was not found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * Constructor with custom message
     * 
     * @param message Custom error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }
}