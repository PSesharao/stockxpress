package com.seshrao.stockxpress.common.exception;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when validation fails.
 * Results in HTTP 400 BAD REQUEST response.
 * 
 * @author StockXpress Team
 */
@Getter
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    /**
     * Constructor with single error message
     * 
     * @param message The validation error message
     */
    public ValidationException(String message) {
        super(message);
        this.errors = new ArrayList<>();
        this.errors.add(message);
    }

    /**
     * Constructor with multiple validation errors
     * 
     * @param errors List of validation error messages
     */
    public ValidationException(List<String> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    /**
     * Constructor with message and errors list
     * 
     * @param message Main error message
     * @param errors List of specific validation errors
     */
    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
}