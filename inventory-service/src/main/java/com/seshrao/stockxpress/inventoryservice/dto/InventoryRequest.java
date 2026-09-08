package com.seshrao.stockxpress.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Data Transfer Object for inventory check request.
 * Contains list of SKU codes to check for availability.
 * 
 * @author StockXpress Team
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryRequest {

    /**
     * List of SKU codes to check for inventory availability
     */
    @NotEmpty(message = "SKU code list cannot be empty")
    @Size(max = 100, message = "Cannot check more than 100 SKU codes at once")
    private List<String> skuCodes;
}
