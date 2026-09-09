package com.seshrao.stockxpress.orderservice.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * OrderLineItem Value Object (DDD)
 * <p>
 * Represents a single item in an order with business validation:
 * - SKU code is required and immutable
 * - Price must be positive
 * - Quantity must be at least 1
 * - Line total is calculated (price * quantity)
 * <p>
 * Business Rules:
 * 1. Cannot have zero or negative quantity
 * 2. Cannot have zero or negative price
 * 3. SKU code must be valid format
 */
@Entity
@Table(name = "t_order_line_items", indexes = {
        @Index(name = "idx_line_sku", columnList = "sku_code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requirement
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Force use of builder
@Builder
@ToString
@EqualsAndHashCode(of = {"skuCode", "price", "quantity"}) // Value Object equality
public class OrderLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_code", nullable = false, length = 100)
    @NotBlank(message = "SKU code is required")
    private String skuCode;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // ==================== Factory Method ====================

    /**
     * Creates a line item with validation.
     *
     * @param skuCode  Product SKU
     * @param price    Unit price
     * @param quantity Quantity ordered
     * @return Validated OrderLineItem
     * @throws IllegalArgumentException if validation fails
     */
    public static OrderLineItem create(String skuCode, BigDecimal price, Integer quantity) {
        validateSkuCode(skuCode);
        validatePrice(price);
        validateQuantity(quantity);

        return OrderLineItem.builder()
                .skuCode(skuCode)
                .price(price)
                .quantity(quantity)
                .build();
    }

    // ==================== Business Logic Methods ====================

    /**
     * Calculates line total (price * quantity).
     * Business rule: Total = unit price × quantity
     *
     * @return Line item total amount
     */
    public BigDecimal getLineTotal() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Increases quantity by specified amount.
     * Business rule: Quantity cannot be negative after increase.
     *
     * @param additionalQuantity Amount to add
     * @throws IllegalArgumentException if result would be negative
     */
    public void increaseQuantity(int additionalQuantity) {
        if (additionalQuantity < 0) {
            throw new IllegalArgumentException("Cannot increase quantity by negative amount");
        }
        this.quantity += additionalQuantity;
    }

    /**
     * Decreases quantity by specified amount.
     * Business rule: Quantity cannot go below 1.
     *
     * @param reductionAmount Amount to subtract
     * @throws IllegalArgumentException if result would be less than 1
     */
    public void decreaseQuantity(int reductionAmount) {
        if (this.quantity - reductionAmount < 1) {
            throw new IllegalArgumentException(
                    String.format("Cannot reduce quantity below 1. Current: %d, Reduction: %d",
                            quantity, reductionAmount)
            );
        }
        this.quantity -= reductionAmount;
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

    private static void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    String.format("Price must be positive. Got: %s", price)
            );
        }
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException(
                    String.format("Quantity must be at least 1. Got: %d", quantity)
            );
        }
    }
}
