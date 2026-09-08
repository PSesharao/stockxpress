package com.seshrao.stockxpress.inventoryservice.exception;

import com.seshrao.stockxpress.common.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Exception thrown when inventory records are not found for requested SKU codes.
 * Extends common-lib ResourceNotFoundException for consistent error handling.
 * 
 * @author StockXpress Team
 */
public class InventoryNotFoundException extends ResourceNotFoundException {

    private final List<String> skuCodes;

    /**
     * Constructor with list of SKU codes not found
     * 
     * @param skuCodes List of SKU codes that were not found in inventory
     */
    public InventoryNotFoundException(List<String> skuCodes) {
        super("Inventory", "skuCodes", skuCodes);
        this.skuCodes = skuCodes;
    }

    /**
     * Constructor with single SKU code
     * 
     * @param skuCode SKU code that was not found
     */
    public InventoryNotFoundException(String skuCode) {
        super("Inventory", "skuCode", skuCode);
        this.skuCodes = List.of(skuCode);
    }

    /**
     * Constructor with custom message
     * 
     * @param message Custom error message
     */
    public InventoryNotFoundException(String message, List<String> skuCodes) {
        super(message);
        this.skuCodes = skuCodes;
    }

    public List<String> getSkuCodes() {
        return skuCodes;
    }
}
