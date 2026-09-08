package com.seshrao.stockxpress.common.exception;

import lombok.Getter;

/**
 * Exception thrown when a business rule is violated.
 * Results in HTTP 422 UNPROCESSABLE ENTITY response.
 * 
 * @author StockXpress Team
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    /**
     * Constructor with error message
     * 
     * @param message The error message
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
        this.args = null;
    }

    /**
     * Constructor with error code and message
     * 
     * @param errorCode Application-specific error code
     * @param message The error message
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    /**
     * Constructor with error code, message, and arguments
     * 
     * @param errorCode Application-specific error code
     * @param message The error message
     * @param args Arguments for message formatting
     */
    public BusinessException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * Constructor with message and cause
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.args = null;
    }
}