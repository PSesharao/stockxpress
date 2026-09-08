package com.seshrao.stockxpress.common.exception;

import lombok.Getter;

/**
 * Exception thrown when an external service is unavailable.
 * Results in HTTP 503 SERVICE UNAVAILABLE response.
 * 
 * @author StockXpress Team
 */
@Getter
public class ServiceUnavailableException extends RuntimeException {

    private final String serviceName;
    private final String operation;

    /**
     * Constructor with service details
     * 
     * @param serviceName The name of the unavailable service
     * @param message The error message
     */
    public ServiceUnavailableException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.operation = null;
    }

    /**
     * Constructor with service details and operation
     * 
     * @param serviceName The name of the unavailable service
     * @param operation The operation that failed
     * @param message The error message
     */
    public ServiceUnavailableException(String serviceName, String operation, String message) {
        super(message);
        this.serviceName = serviceName;
        this.operation = operation;
    }

    /**
     * Constructor with service details and cause
     * 
     * @param serviceName The name of the unavailable service
     * @param message The error message
     * @param cause The underlying cause
     */
    public ServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.operation = null;
    }

    /**
     * Constructor with full details
     * 
     * @param serviceName The name of the unavailable service
     * @param operation The operation that failed
     * @param message The error message
     * @param cause The underlying cause
     */
    public ServiceUnavailableException(String serviceName, String operation, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.operation = operation;
    }
}