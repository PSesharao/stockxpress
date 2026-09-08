package com.seshrao.stockxpress.inventoryservice.service;

import com.seshrao.stockxpress.inventoryservice.dto.InventoryResponse;
import com.seshrao.stockxpress.inventoryservice.model.Inventory;

import java.util.List;

/**
 * Service interface for inventory management operations.
 * Defines the contract for checking stock availability and managing inventory.
 * 
 * @author StockXpress Team
 */
public interface IInventoryService {

    /**
     * Check if products are in stock for the given SKU codes.
     * Results are cached to improve performance for frequent queries.
     * 
     * @param skuCodes List of SKU codes to check
     * @return List of InventoryResponse containing stock availability for each SKU
     * @throws IllegalArgumentException if skuCodes is null or empty
     */
    List<InventoryResponse> isInStock(List<String> skuCodes);

    /**
     * Get inventory details by SKU code.
     * 
     * @param skuCode SKU code to search for
     * @return Inventory entity
     * @throws com.seshrao.stockxpress.common.exception.ResourceNotFoundException if inventory not found
     */
    Inventory getInventoryBySkuCode(String skuCode);

    /**
     * Update inventory quantity for a specific SKU.
     * 
     * @param skuCode SKU code to update
     * @param quantity New quantity
     * @return Updated inventory
     * @throws com.seshrao.stockxpress.common.exception.ResourceNotFoundException if inventory not found
     */
    Inventory updateInventory(String skuCode, Integer quantity);

    /**
     * Create new inventory record.
     * 
     * @param inventory Inventory entity to create
     * @return Created inventory
     */
    Inventory createInventory(Inventory inventory);

    /**
     * Get all inventory records.
     * 
     * @return List of all inventory records
     */
    List<Inventory> getAllInventory();

    /**
     * Delete inventory by SKU code.
     * 
     * @param skuCode SKU code to delete
     * @throws com.seshrao.stockxpress.common.exception.ResourceNotFoundException if inventory not found
     */
    void deleteInventory(String skuCode);
}
