package com.seshrao.stockxpress.inventoryservice.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Inventory Domain Model - Rich Domain Model (DDD)
 * <p>
 * Encapsulates inventory business logic and invariants:
 * - Stock quantity cannot be negative
 * - SKU code is unique and immutable
 * - Stock reservation and release operations
 * - Low stock detection
 * <p>
 * Business Rules Enforced:
 * 1. Quantity must always be >= 0
 * 2. Cannot reserve more stock than available
 * 3. Cannot release more stock than reserved
 * 4. SKU code must be valid format
 * 5. Track last restocked date for inventory management
 *
 * @author StockXpress Team
 */
@Entity
@Table(name = "t_inventory", indexes = {
        @Index(name = "idx_sku_code", columnList = "sku_code", unique = true),
        @Index(name = "idx_quantity", columnList = "quantity"),
        @Index(name = "idx_last_restocked", columnList = "last_restocked_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requirement
@ToString
@EqualsAndHashCode(of = "skuCode") // Business key equality
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int LOW_STOCK_THRESHOLD = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_code", nullable = false, unique = true, length = 100)
    @NotBlank(message = "SKU code cannot be blank")
    private String skuCode;

    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    @Min(value = 0, message = "Reserved quantity must be non-negative")
    private Integer reservedQuantity = 0;

    @Column(name = "last_restocked_date")
    private LocalDateTime lastRestockedDate;

    @Column(name = "minimum_stock_level")
    @Min(value = 0, message = "Minimum stock level must be non-negative")
    private Integer minimumStockLevel = LOW_STOCK_THRESHOLD;

    // ==================== Factory Methods ====================

    /**
     * Creates a new inventory entry with validation.
     * Business rule: Initial quantity must be non-negative.
     *
     * @param skuCode         Product SKU
     * @param initialQuantity Starting stock quantity
     * @return New Inventory instance
     * @throws IllegalArgumentException if validation fails
     */
    public static Inventory create(String skuCode, Integer initialQuantity) {
        validateSkuCode(skuCode);
        validateQuantity(initialQuantity);

        Inventory inventory = new Inventory();
        inventory.skuCode = skuCode;
        inventory.quantity = initialQuantity;
        inventory.reservedQuantity = 0;
        inventory.lastRestockedDate = LocalDateTime.now();
        inventory.minimumStockLevel = LOW_STOCK_THRESHOLD;

        return inventory;
    }

    // ==================== Business Logic Methods ====================

    /**
     * Checks if product is in stock.
     * Business rule: Item is in stock if available quantity > 0.
     * Available = total quantity - reserved quantity.
     *
     * @return true if available quantity > 0
     */
    public boolean isInStock() {
        return getAvailableQuantity() > 0;
    }

    /**
     * Checks if sufficient quantity is available.
     * Business rule: Available quantity must be >= requested amount.
     *
     * @param requestedQuantity Amount to check
     * @return true if enough stock available
     */
    public boolean hasStock(int requestedQuantity) {
        return getAvailableQuantity() >= requestedQuantity;
    }

    /**
     * Gets available (unreserved) quantity.
     *
     * @return Quantity available for orders
     */
    public Integer getAvailableQuantity() {
        return quantity - (reservedQuantity != null ? reservedQuantity : 0);
    }

    /**
     * Checks if stock is below minimum level (needs reordering).
     * Business rule: Alert when stock falls below threshold.
     *
     * @return true if quantity <= minimum stock level
     */
    public boolean isLowStock() {
        return quantity <= minimumStockLevel;
    }

    /**
     * Reserves stock for an order.
     * Business rule: Cannot reserve more than available.
     *
     * @param quantityToReserve Amount to reserve
     * @throws IllegalStateException if insufficient stock
     */
    public void reserveStock(int quantityToReserve) {
        if (quantityToReserve < 0) {
            throw new IllegalArgumentException("Cannot reserve negative quantity");
        }

        if (!hasStock(quantityToReserve)) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for SKU %s. Available: %d, Requested: %d",
                            skuCode, getAvailableQuantity(), quantityToReserve)
            );
        }

        this.reservedQuantity += quantityToReserve;
    }

    /**
     * Releases previously reserved stock (e.g., when order is cancelled).
     * Business rule: Cannot release more than reserved.
     *
     * @param quantityToRelease Amount to release
     * @throws IllegalArgumentException if trying to release more than reserved
     */
    public void releaseStock(int quantityToRelease) {
        if (quantityToRelease < 0) {
            throw new IllegalArgumentException("Cannot release negative quantity");
        }

        if (quantityToRelease > this.reservedQuantity) {
            throw new IllegalStateException(
                    String.format("Cannot release %d units. Only %d reserved for SKU %s",
                            quantityToRelease, this.reservedQuantity, skuCode)
            );
        }

        this.reservedQuantity -= quantityToRelease;
    }

    /**
     * Decrements stock (actual consumption after shipping).
     * Business rule: Cannot decrement below zero.
     *
     * @param quantityToDecrement Amount to remove
     * @throws IllegalStateException if result would be negative
     */
    public void decrementStock(int quantityToDecrement) {
        if (quantityToDecrement < 0) {
            throw new IllegalArgumentException("Cannot decrement by negative quantity");
        }

        if (this.quantity < quantityToDecrement) {
            throw new IllegalStateException(
                    String.format("Cannot decrement %d units. Only %d available for SKU %s",
                            quantityToDecrement, this.quantity, skuCode)
            );
        }

        this.quantity -= quantityToDecrement;

        // Also decrement reserved if applicable
        if (this.reservedQuantity >= quantityToDecrement) {
            this.reservedQuantity -= quantityToDecrement;
        } else {
            this.reservedQuantity = 0;
        }
    }

    /**
     * Restocks inventory.
     * Business rule: Can only add positive quantities.
     *
     * @param quantityToAdd Amount to add
     * @throws IllegalArgumentException if quantity is negative or zero
     */
    public void restock(int quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException(
                    String.format("Restock quantity must be positive. Got: %d", quantityToAdd)
            );
        }

        this.quantity += quantityToAdd;
        this.lastRestockedDate = LocalDateTime.now();
    }

    /**
     * Sets minimum stock level for reorder alerts.
     *
     * @param minimumLevel Minimum quantity threshold
     * @throws IllegalArgumentException if level is negative
     */
    public void setMinimumStockLevel(int minimumLevel) {
        if (minimumLevel < 0) {
            throw new IllegalArgumentException("Minimum stock level cannot be negative");
        }
        this.minimumStockLevel = minimumLevel;
    }

    /**
     * Adjusts inventory to match physical count (inventory reconciliation).
     * Business rule: Physical count cannot be less than reserved quantity.
     *
     * @param physicalCount Actual count from warehouse
     * @throws IllegalStateException if physical count < reserved quantity
     */
    public void reconcileInventory(int physicalCount) {
        if (physicalCount < 0) {
            throw new IllegalArgumentException("Physical count cannot be negative");
        }

        if (physicalCount < this.reservedQuantity) {
            throw new IllegalStateException(
                    String.format("Physical count (%d) cannot be less than reserved quantity (%d) for SKU %s",
                            physicalCount, this.reservedQuantity, skuCode)
            );
        }

        this.quantity = physicalCount;
    }

    // ==================== Validation Methods ====================

    private static void validateSkuCode(String skuCode) {
        if (skuCode == null || skuCode.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU code cannot be null or empty");
        }
        if (!skuCode.matches("^[A-Z0-9_]+$")) {
            throw new IllegalArgumentException(
                    String.format("Invalid SKU code format: %s. Must contain only uppercase letters, numbers, and underscores", skuCode)
            );
        }
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException(
                    String.format("Quantity must be non-negative. Got: %d", quantity)
            );
        }
    }
}
