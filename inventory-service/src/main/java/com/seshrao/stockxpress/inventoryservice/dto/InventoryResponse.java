package com.seshrao.stockxpress.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Data Transfer Object for inventory availability response.
 * Contains SKU code and stock availability information.
 * Implements Serializable for Redis caching support.
 *
 * @author StockXpress Team
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * SKU (Stock Keeping Unit) code - unique identifier for the product
     */
    @NotBlank(message = "SKU code cannot be blank")
    private String skuCode;

    /**
     * Indicates whether the product is in stock (quantity > 0)
     */
    @NotNull(message = "Stock availability status cannot be null")
    private Boolean isInStock;

    /**
     * Available quantity in stock (optional, for detailed information)
     */
    private Integer availableQuantity;
}
