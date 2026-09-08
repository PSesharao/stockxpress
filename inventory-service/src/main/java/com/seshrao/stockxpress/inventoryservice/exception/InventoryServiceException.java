package com.seshrao.stockxpress.inventoryservice.exception;

import com.seshrao.stockxpress.common.exception.BusinessException;

/**
 * General business exception for inventory service operations.
 * Extends common-lib BusinessException for consistent error handling.
 * 
 * @author StockXpress Team
 */
public class InventoryServiceException extends BusinessException {

    /**
     * Constructor with error code and message
     * 
     * @param errorCode Specific error code for the business exception
     * @param message Error message
     */
    public InventoryServiceException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructor with error code, message, and cause
     * 
     * @param errorCode Specific error code
     * @param message Error message
     * @param cause Root cause exception
     */
    public InventoryServiceException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * Constructor with just message (uses default error code)
     * 
     * @param message Error message
     */
    public InventoryServiceException(String message) {
        super("INVENTORY_ERROR", message);
    }
}
