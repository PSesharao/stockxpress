package com.seshrao.stockxpress.inventoryservice.mapper;

import com.seshrao.stockxpress.inventoryservice.dto.InventoryResponse;
import com.seshrao.stockxpress.inventoryservice.model.Inventory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component for converting between Inventory entities and DTOs.
 * Provides clean separation of concerns for data transformation.
 * 
 * @author StockXpress Team
 */
@Component
public class InventoryMapper {

    /**
     * Convert Inventory entity to InventoryResponse DTO.
     * 
     * @param inventory Inventory entity
     * @return InventoryResponse DTO with stock availability information
     */
    public InventoryResponse toInventoryResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .isInStock(inventory.getQuantity() != null && inventory.getQuantity() > 0)
                .availableQuantity(inventory.getQuantity())
                .build();
    }

    /**
     * Convert list of Inventory entities to list of InventoryResponse DTOs.
     * 
     * @param inventories List of Inventory entities
     * @return List of InventoryResponse DTOs
     */
    public List<InventoryResponse> toInventoryResponseList(List<Inventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return List.of();
        }
        
        return inventories.stream()
                .map(this::toInventoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert InventoryResponse DTO to Inventory entity.
     * Used for creating or updating inventory records.
     * 
     * @param response InventoryResponse DTO
     * @return Inventory entity
     */
    public Inventory toInventory(InventoryResponse response) {
        if (response == null) {
            return null;
        }
        
        return Inventory.builder()
                .skuCode(response.getSkuCode())
                .quantity(response.getAvailableQuantity())
                .build();
    }
}
