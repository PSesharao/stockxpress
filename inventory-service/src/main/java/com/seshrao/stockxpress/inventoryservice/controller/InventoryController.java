package com.seshrao.stockxpress.inventoryservice.controller;

import com.seshrao.stockxpress.common.annotation.LogExecutionTime;
import com.seshrao.stockxpress.inventoryservice.dto.InventoryRequest;
import com.seshrao.stockxpress.inventoryservice.dto.InventoryResponse;
import com.seshrao.stockxpress.inventoryservice.model.Inventory;
import com.seshrao.stockxpress.inventoryservice.service.IInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * REST Controller for inventory management operations.
 * Provides endpoints for checking stock availability and managing inventory.
 * All exceptions are handled by GlobalExceptionHandler from common-lib.
 *
 * @author StockXpress Team
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@Validated
public class InventoryController {

    private final IInventoryService inventoryService;

    /**
     * Check if products are in stock for the given SKU codes.
     *
     * @param skuCode List of SKU codes to check
     * @return ResponseEntity with list of InventoryResponse
     * <p>
     * Example: GET /api/inventory?skuCode=Iphone_13&skuCode=Iphone_13_red
     */
    @GetMapping
    @LogExecutionTime(value = "Check inventory endpoint", warnThresholdMillis = 1500)
    public ResponseEntity<List<InventoryResponse>> isInStock(
            @RequestParam("skuCode")
            @NotEmpty(message = "SKU code list cannot be empty")
            List<String> skuCode) {

        log.info("Received request to check inventory for {} SKU codes", skuCode.size());

        List<InventoryResponse> response = inventoryService.isInStock(skuCode);

        log.info("Returning inventory status for {} items", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Check if products are in stock using request body.
     * Alternative endpoint that accepts JSON body.
     *
     * @param request InventoryRequest containing SKU codes
     * @return ResponseEntity with list of InventoryResponse
     * <p>
     * Example: POST /api/inventory/check
     * Body: { "skuCodes": ["Iphone_13", "Iphone_13_red"] }
     */
    @PostMapping("/check")
    @LogExecutionTime(value = "Check inventory POST endpoint")
    public ResponseEntity<List<InventoryResponse>> checkInventory(
            @Valid @RequestBody InventoryRequest request) {

        log.info("Received POST request to check inventory for {} SKU codes", request.getSkuCodes().size());

        List<InventoryResponse> response = inventoryService.isInStock(request.getSkuCodes());

        log.info("Returning inventory status for {} items", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get inventory by SKU code.
     *
     * @param skuCode SKU code to search for
     * @return ResponseEntity with Inventory entity
     * <p>
     * Example: GET /api/inventory/Iphone_13
     */
    @GetMapping("/{skuCode}")
    @LogExecutionTime(value = "Get inventory by SKU code endpoint")
    public ResponseEntity<Inventory> getInventoryBySkuCode(
            @PathVariable @NotBlank(message = "SKU code cannot be blank") String skuCode) {

        log.info("Received request to get inventory for SKU code: {}", skuCode);

        Inventory inventory = inventoryService.getInventoryBySkuCode(skuCode);

        log.info("Returning inventory details for SKU code: {}", skuCode);
        return ResponseEntity.ok(inventory);
    }

    /**
     * Get all inventory records.
     *
     * @return ResponseEntity with list of all Inventory entities
     * <p>
     * Example: GET /api/inventory/all
     */
    @GetMapping("/all")
    @LogExecutionTime(value = "Get all inventory endpoint", warnThresholdMillis = 2000)
    public ResponseEntity<List<Inventory>> getAllInventory() {
        log.info("Received request to get all inventory records");

        List<Inventory> inventories = inventoryService.getAllInventory();

        log.info("Returning {} inventory records", inventories.size());
        return ResponseEntity.ok(inventories);
    }

    /**
     * Create new inventory record.
     *
     * @param inventory Inventory entity to create
     * @return ResponseEntity with created Inventory entity
     * <p>
     * Example: POST /api/inventory
     * Body: { "skuCode": "Iphone_14", "quantity": 50 }
     */
    @PostMapping
    @LogExecutionTime(value = "Create inventory endpoint")
    public ResponseEntity<Inventory> createInventory(@Valid @RequestBody Inventory inventory) {
        log.info("Received request to create inventory for SKU code: {}", inventory.getSkuCode());

        Inventory createdInventory = inventoryService.createInventory(inventory);

        log.info("Successfully created inventory with ID: {}", createdInventory.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdInventory);
    }

    /**
     * Update inventory quantity.
     *
     * @param skuCode  SKU code to update
     * @param quantity New quantity
     * @return ResponseEntity with updated Inventory entity
     * <p>
     * Example: PUT /api/inventory/Iphone_13?quantity=150
     */
    @PutMapping("/{skuCode}")
    @LogExecutionTime(value = "Update inventory endpoint")
    public ResponseEntity<Inventory> updateInventory(
            @PathVariable @NotBlank(message = "SKU code cannot be blank") String skuCode,
            @RequestParam Integer quantity) {

        log.info("Received request to update inventory for SKU code: {} with quantity: {}", skuCode, quantity);

        Inventory updatedInventory = inventoryService.updateInventory(skuCode, quantity);

        log.info("Successfully updated inventory for SKU code: {}", skuCode);
        return ResponseEntity.ok(updatedInventory);
    }

    /**
     * Delete inventory by SKU code.
     *
     * @param skuCode SKU code to delete
     * @return ResponseEntity with no content
     * <p>
     * Example: DELETE /api/inventory/Iphone_13
     */
    @DeleteMapping("/{skuCode}")
    @LogExecutionTime(value = "Delete inventory endpoint")
    public ResponseEntity<Void> deleteInventory(
            @PathVariable @NotBlank(message = "SKU code cannot be blank") String skuCode) {

        log.info("Received request to delete inventory for SKU code: {}", skuCode);

        inventoryService.deleteInventory(skuCode);

        log.info("Successfully deleted inventory for SKU code: {}", skuCode);
        return ResponseEntity.noContent().build();
    }
}
