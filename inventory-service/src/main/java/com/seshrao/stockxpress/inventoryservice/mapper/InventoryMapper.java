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
     * Uses domain business logic methods for stock availability.
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
                .isInStock(inventory.isInStock()) // Use domain method
                .availableQuantity(inventory.getAvailableQuantity()) // Use domain method
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
     * Uses domain factory method to ensure business validation.
     *
     * @param response InventoryResponse DTO
     * @return Inventory entity with business validation enforced
     */
    public Inventory toInventory(InventoryResponse response) {
        if (response == null) {
            return null;
        }

        // Use domain factory method with validation
        return Inventory.create(
                response.getSkuCode(),
                response.getAvailableQuantity() != null ? response.getAvailableQuantity() : 0
        );
    }
}
