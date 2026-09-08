package com.seshrao.stockxpress.common.exception;

import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when inventory is insufficient to fulfill an order.
 * Results in HTTP 409 CONFLICT response.
 * 
 * @author StockXpress Team
 */
@Getter
public class InsufficientInventoryException extends BusinessException {

    private final List<String> unavailableSkus;

    /**
     * Constructor with unavailable SKUs
     * 
     * @param unavailableSkus List of SKU codes that are out of stock
     */
    public InsufficientInventoryException(List<String> unavailableSkus) {
        super("INSUFFICIENT_INVENTORY", "Insufficient inventory for requested items");
        this.unavailableSkus = unavailableSkus;
    }

    /**
     * Constructor with custom message and unavailable SKUs
     * 
     * @param message Custom error message
     * @param unavailableSkus List of SKU codes that are out of stock
     */
    public InsufficientInventoryException(String message, List<String> unavailableSkus) {
        super("INSUFFICIENT_INVENTORY", message);
        this.unavailableSkus = unavailableSkus;
    }
}