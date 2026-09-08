package com.seshrao.stockxpress.common.enums;

/**
 * Enum representing application-specific error codes.
 */
public enum ErrorCode {
    
    // Validation Errors (1000-1999)
    VALIDATION_FAILED("ERR-1000", "Validation failed"),
    INVALID_INPUT("ERR-1001", "Invalid input provided"),
    MISSING_REQUIRED_FIELD("ERR-1002", "Required field is missing"),
    INVALID_FORMAT("ERR-1003", "Invalid format"),
    
    // Resource Errors (2000-2999)
    RESOURCE_NOT_FOUND("ERR-2000", "Resource not found"),
    PRODUCT_NOT_FOUND("ERR-2001", "Product not found"),
    ORDER_NOT_FOUND("ERR-2002", "Order not found"),
    INVENTORY_NOT_FOUND("ERR-2003", "Inventory not found"),
    USER_NOT_FOUND("ERR-2004", "User not found"),
    
    // Business Logic Errors (3000-3999)
    INSUFFICIENT_STOCK("ERR-3000", "Insufficient stock available"),
    INVALID_ORDER_STATUS("ERR-3001", "Invalid order status transition"),
    ORDER_ALREADY_CANCELLED("ERR-3002", "Order has already been cancelled"),
    ORDER_CANNOT_BE_MODIFIED("ERR-3003", "Order cannot be modified in current status"),
    DUPLICATE_RESOURCE("ERR-3004", "Resource already exists"),
    PRICE_MISMATCH("ERR-3005", "Price mismatch detected"),
    
    // Service Errors (4000-4999)
    SERVICE_UNAVAILABLE("ERR-4000", "Service is temporarily unavailable"),
    EXTERNAL_SERVICE_ERROR("ERR-4001", "External service error"),
    DATABASE_ERROR("ERR-4002", "Database operation failed"),
    MESSAGING_ERROR("ERR-4003", "Message publishing/consuming failed"),
    
    // Authentication & Authorization Errors (5000-5999)
    UNAUTHORIZED("ERR-5000", "Unauthorized access"),
    FORBIDDEN("ERR-5001", "Access forbidden"),
    INVALID_TOKEN("ERR-5002", "Invalid or expired token"),
    INSUFFICIENT_PERMISSIONS("ERR-5003", "Insufficient permissions"),
    
    // System Errors (9000-9999)
    INTERNAL_SERVER_ERROR("ERR-9000", "Internal server error"),
    CONFIGURATION_ERROR("ERR-9001", "Configuration error"),
    UNKNOWN_ERROR("ERR-9999", "Unknown error occurred");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Returns formatted error message with code.
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s", code, message);
    }
}