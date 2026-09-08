package com.seshrao.stockxpress.inventoryservice.exception;

import com.seshrao.stockxpress.common.exception.ValidationException;

import java.util.List;

/**
 * Exception thrown when inventory request contains invalid data.
 * Extends common-lib ValidationException for consistent validation error handling.
 * 
 * @author StockXpress Team
 */
public class InvalidInventoryRequestException extends ValidationException {

    /**
     * Constructor with validation errors
     * 
     * @param errors List of validation error messages
     */
    public InvalidInventoryRequestException(List<String> errors) {
        super(errors);
    }

    /**
     * Constructor with single error message
     * 
     * @param message Validation error message
     */
    public InvalidInventoryRequestException(String message) {
        super(List.of(message));
    }
}
