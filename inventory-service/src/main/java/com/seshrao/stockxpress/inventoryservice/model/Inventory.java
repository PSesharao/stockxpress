package com.seshrao.stockxpress.inventoryservice.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Entity representing inventory stock information.
 * Includes database indexes for optimized queries on skuCode field.
 * Implements Serializable for Redis caching support.
 *
 * @author StockXpress Team
 */
@Entity
@Table(name = "t_inventory", indexes = {
        @Index(name = "idx_sku_code", columnList = "sku_code", unique = true),
        @Index(name = "idx_quantity", columnList = "quantity")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key - auto-generated
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SKU (Stock Keeping Unit) code - unique identifier for the product
     */
    @Column(name = "sku_code", nullable = false, unique = true, length = 100)
    @NotBlank(message = "SKU code cannot be blank")
    private String skuCode;

    /**
     * Available quantity in stock
     */
    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer quantity;
}
