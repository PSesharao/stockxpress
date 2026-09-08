package com.seshrao.stockxpress.productservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Product Response DTO with validation constraints.
 * Used for returning product data from API endpoints.
 * <p>
 * Validation Rules:
 * - ID: Must be present for existing products
 * - Name: 3-100 characters, non-blank
 * - Description: 10-500 characters, non-blank
 * - Price: Must be greater than 0.01
 *
 * @author StockXpress Team
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    /**
     * Unique identifier for the product (MongoDB ObjectId)
     */
    @NotBlank(message = "Product ID is required")
    private String id;

    /**
     * Product name
     */
    @NotBlank(message = "Product name is required and cannot be blank")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
    private String name;

    /**
     * Product description
     */
    @NotBlank(message = "Product description is required and cannot be blank")
    @Size(min = 10, max = 500, message = "Product description must be between 10 and 500 characters")
    private String description;

    /**
     * Product price in USD
     */
    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.01", message = "Product price must be greater than 0")
    private BigDecimal price;
}
