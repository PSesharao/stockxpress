package com.seshrao.stockxpress.orderservice.client;

import com.seshrao.stockxpress.orderservice.dto.InventoryResponse;

import java.util.List;

/**
 * Client interface for communicating with Inventory Service.
 * Abstracts the inventory checking logic from the order service.
 */
public interface InventoryClient {
    
    /**
     * Checks inventory availability for the given SKU codes.
     *
     * @param skuCodes List of SKU codes to check
     * @return List of inventory responses indicating stock availability
     * @throws com.seshrao.stockxpress.common.exception.ServiceUnavailableException if inventory service is unavailable
     */
    List<InventoryResponse> checkInventory(List<String> skuCodes);
}
