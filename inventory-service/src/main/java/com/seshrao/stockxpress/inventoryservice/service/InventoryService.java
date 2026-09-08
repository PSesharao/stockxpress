package com.seshrao.stockxpress.inventoryservice.service;

import com.seshrao.stockxpress.common.annotation.LogExecutionTime;
import com.seshrao.stockxpress.common.exception.ResourceNotFoundException;
import com.seshrao.stockxpress.inventoryservice.dto.InventoryResponse;
import com.seshrao.stockxpress.inventoryservice.mapper.InventoryMapper;
import com.seshrao.stockxpress.inventoryservice.model.Inventory;
import com.seshrao.stockxpress.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for inventory management operations.
 * Implements caching for improved performance and uses custom exceptions
 * from common-lib for consistent error handling.
 *
 * @author StockXpress Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService implements IInventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    /**
     * Check if products are in stock for the given SKU codes.
     * Results are cached for 1 hour to improve performance.
     *
     * @param skuCodes List of SKU codes to check
     * @return List of InventoryResponse containing stock availability
     * @throws IllegalArgumentException if skuCodes is null or empty
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "inventoryCache", key = "#skuCodes.hashCode()")
    @LogExecutionTime(value = "Check inventory stock availability", warnThresholdMillis = 1000)
    public List<InventoryResponse> isInStock(List<String> skuCodes) {
        if (CollectionUtils.isEmpty(skuCodes)) {
            log.warn("Attempted to check inventory with empty SKU code list");
            throw new IllegalArgumentException("SKU code list cannot be null or empty");
        }

        log.info("Checking inventory for {} SKU codes", skuCodes.size());

        List<Inventory> inventories = inventoryRepository.findBySkuCodeIn(skuCodes);

        if (CollectionUtils.isEmpty(inventories)) {
            log.warn("No inventory found for SKU codes: {}", skuCodes);
        } else {
            log.info("Found {} inventory records for {} SKU codes", inventories.size(), skuCodes.size());
        }

        return inventoryMapper.toInventoryResponseList(inventories);
    }

    /**
     * Get inventory details by SKU code.
     *
     * @param skuCode SKU code to search for
     * @return Inventory entity
     * @throws ResourceNotFoundException if inventory not found
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "inventoryCache", key = "#skuCode")
    @LogExecutionTime(value = "Get inventory by SKU code")
    public Inventory getInventoryBySkuCode(String skuCode) {
        if (skuCode == null || skuCode.trim().isEmpty()) {
            log.warn("Attempted to get inventory with null or empty SKU code");
            throw new IllegalArgumentException("SKU code cannot be null or empty");
        }

        log.info("Retrieving inventory for SKU code: {}", skuCode);

        return inventoryRepository.findBySkuCodeIn(List.of(skuCode))
                .stream()
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Inventory not found for SKU code: {}", skuCode);
                    return new ResourceNotFoundException("Inventory", "skuCode", skuCode);
                });
    }

    /**
     * Update inventory quantity for a specific SKU.
     * Evicts cache to ensure fresh data on next read.
     *
     * @param skuCode  SKU code to update
     * @param quantity New quantity
     * @return Updated inventory
     * @throws ResourceNotFoundException if inventory not found
     */
    @Override
    @Transactional
    @CacheEvict(value = "inventoryCache", allEntries = true)
    @LogExecutionTime(value = "Update inventory", logParameters = true)
    public Inventory updateInventory(String skuCode, Integer quantity) {
        if (skuCode == null || skuCode.trim().isEmpty()) {
            log.warn("Attempted to update inventory with null or empty SKU code");
            throw new IllegalArgumentException("SKU code cannot be null or empty");
        }

        if (quantity == null || quantity < 0) {
            log.warn("Attempted to update inventory with invalid quantity: {}", quantity);
            throw new IllegalArgumentException("Quantity must be a non-negative number");
        }

        log.info("Updating inventory for SKU code: {} with quantity: {}", skuCode, quantity);

        Inventory inventory = getInventoryBySkuCode(skuCode);
        inventory.setQuantity(quantity);

        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Successfully updated inventory for SKU code: {}", skuCode);

        return updatedInventory;
    }

    /**
     * Create new inventory record.
     * Evicts cache to ensure fresh data.
     *
     * @param inventory Inventory entity to create
     * @return Created inventory
     */
    @Override
    @Transactional
    @CacheEvict(value = "inventoryCache", allEntries = true)
    @LogExecutionTime(value = "Create inventory")
    public Inventory createInventory(Inventory inventory) {
        if (inventory == null) {
            log.warn("Attempted to create null inventory");
            throw new IllegalArgumentException("Inventory cannot be null");
        }

        if (inventory.getSkuCode() == null || inventory.getSkuCode().trim().isEmpty()) {
            log.warn("Attempted to create inventory with null or empty SKU code");
            throw new IllegalArgumentException("SKU code cannot be null or empty");
        }

        log.info("Creating new inventory for SKU code: {}", inventory.getSkuCode());

        Inventory createdInventory = inventoryRepository.save(inventory);
        log.info("Successfully created inventory with ID: {} for SKU code: {}",
                createdInventory.getId(), createdInventory.getSkuCode());

        return createdInventory;
    }

    /**
     * Get all inventory records.
     *
     * @return List of all inventory records
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "inventoryCache", key = "'all'")
    @LogExecutionTime(value = "Get all inventory", warnThresholdMillis = 2000)
    public List<Inventory> getAllInventory() {
        log.info("Retrieving all inventory records");

        List<Inventory> inventories = inventoryRepository.findAll();
        log.info("Found {} inventory records", inventories.size());

        return inventories;
    }

    /**
     * Delete inventory by SKU code.
     * Evicts cache to ensure consistency.
     *
     * @param skuCode SKU code to delete
     * @throws ResourceNotFoundException if inventory not found
     */
    @Override
    @Transactional
    @CacheEvict(value = "inventoryCache", allEntries = true)
    @LogExecutionTime(value = "Delete inventory")
    public void deleteInventory(String skuCode) {
        if (skuCode == null || skuCode.trim().isEmpty()) {
            log.warn("Attempted to delete inventory with null or empty SKU code");
            throw new IllegalArgumentException("SKU code cannot be null or empty");
        }

        log.info("Deleting inventory for SKU code: {}", skuCode);

        Inventory inventory = getInventoryBySkuCode(skuCode);
        inventoryRepository.delete(inventory);

        log.info("Successfully deleted inventory for SKU code: {}", skuCode);
    }
}
