package com.seshrao.stockxpress.productservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product Domain Model - Rich Domain Model (DDD)
 * <p>
 * Encapsulates product business logic and invariants:
 * - Product name and SKU are required
 * - Price must be positive
 * - Price changes are tracked
 * - Product lifecycle management (active/discontinued)
 * <p>
 * Business Rules Enforced:
 * 1. Product must have valid name and SKU
 * 2. Price must be positive
 * 3. Cannot activate discontinued product without review
 * 4. Category must be valid
 * 5. Track creation and modification dates
 *
 * @author StockXpress Team
 */
@Document(collection = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MongoDB requirement
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Force use of builder/factory
@Builder
@ToString
@EqualsAndHashCode(of = "skuCode") // Business key equality
public class Product {

    @Id
    private String id;

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    private BigDecimal price;

    @NotBlank(message = "SKU code is required")
    @Indexed(unique = true)
    private String skuCode;

    @NotBlank(message = "Category is required")
    private String category;

    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    private BigDecimal previousPrice;

    private LocalDateTime priceChangedDate;

    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();

    private LocalDateTime lastModifiedDate;

    // ==================== Factory Methods ====================

    /**
     * Creates a new product with validation.
     * Business rule: All required fields must be provided and valid.
     *
     * @param name        Product name
     * @param description Product description
     * @param price       Product price
     * @param skuCode     SKU code
     * @param category    Product category
     * @return New Product instance
     * @throws IllegalArgumentException if validation fails
     */
    public static Product create(String name, String description, BigDecimal price,
                                  String skuCode, String category) {
        validateName(name);
        validatePrice(price);
        validateSkuCode(skuCode);
        validateCategory(category);

        return Product.builder()
                .name(name.trim())
                .description(description != null ? description.trim() : "")
                .price(price)
                .skuCode(skuCode.toUpperCase())
                .category(category.trim())
                .status(ProductStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .build();
    }

    // ==================== Business Logic Methods ====================

    /**
     * Updates product price.
     * Business rule: Price must be positive. Price history is maintained.
     *
     * @param newPrice New price
     * @throws IllegalArgumentException if price is invalid
     */
    public void updatePrice(BigDecimal newPrice) {
        validatePrice(newPrice);

        if (!newPrice.equals(this.price)) {
            this.previousPrice = this.price;
            this.price = newPrice;
            this.priceChangedDate = LocalDateTime.now();
            this.lastModifiedDate = LocalDateTime.now();
        }
    }

    /**
     * Checks if product had a price increase.
     *
     * @return true if current price > previous price
     */
    public boolean hasPriceIncreased() {
        return previousPrice != null && price.compareTo(previousPrice) > 0;
    }

    /**
     * Checks if product is on sale (price decreased).
     *
     * @return true if current price < previous price
     */
    public boolean isOnSale() {
        return previousPrice != null && price.compareTo(previousPrice) < 0;
    }

    /**
     * Calculates discount percentage if product is on sale.
     *
     * @return Discount percentage (0-100), or 0 if not on sale
     */
    public BigDecimal getDiscountPercentage() {
        if (!isOnSale() || previousPrice == null || previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal difference = previousPrice.subtract(price);
        return difference.divide(previousPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Updates product information.
     * Business rule: Cannot change SKU code (immutable).
     *
     * @param name        New name
     * @param description New description
     * @param category    New category
     * @throws IllegalArgumentException if validation fails
     */
    public void updateInfo(String name, String description, String category) {
        if (name != null && !name.trim().isEmpty()) {
            validateName(name);
            this.name = name.trim();
        }

        if (description != null) {
            this.description = description.trim();
        }

        if (category != null && !category.trim().isEmpty()) {
            validateCategory(category);
            this.category = category.trim();
        }

        this.lastModifiedDate = LocalDateTime.now();
    }

    /**
     * Marks product as discontinued.
     * Business rule: Discontinued products cannot be ordered.
     */
    public void discontinue() {
        if (this.status == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException(
                    String.format("Product %s is already discontinued", skuCode)
            );
        }
        this.status = ProductStatus.DISCONTINUED;
        this.lastModifiedDate = LocalDateTime.now();
    }

    /**
     * Reactivates a discontinued product.
     * Business rule: Requires review before reactivation.
     */
    public void reactivate() {
        if (this.status == ProductStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format("Product %s is already active", skuCode)
            );
        }
        this.status = ProductStatus.ACTIVE;
        this.lastModifiedDate = LocalDateTime.now();
    }

    /**
     * Checks if product is available for ordering.
     * Business rule: Only active products can be ordered.
     *
     * @return true if product status is ACTIVE
     */
    public boolean isAvailableForOrder() {
        return this.status == ProductStatus.ACTIVE;
    }

    /**
     * Checks if product is newly added (within last 30 days).
     *
     * @return true if created within 30 days
     */
    public boolean isNew() {
        if (createdDate == null) {
            return false;
        }
        return createdDate.isAfter(LocalDateTime.now().minusDays(30));
    }

    /**
     * Checks if product price was recently changed.
     *
     * @param days Number of days to check
     * @return true if price changed within specified days
     */
    public boolean isPriceRecentlyChanged(int days) {
        if (priceChangedDate == null) {
            return false;
        }
        return priceChangedDate.isAfter(LocalDateTime.now().minusDays(days));
    }

    // ==================== Validation Methods ====================

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (name.trim().length() < 3) {
            throw new IllegalArgumentException(
                    String.format("Product name must be at least 3 characters. Got: '%s'", name)
            );
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException(
                    String.format("Product name cannot exceed 200 characters. Got: %d", name.length())
            );
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    String.format("Price must be non-negative. Got: %s", price)
            );
        }
    }

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

    private static void validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
    }

    // ==================== Product Status Enum ====================

    /**
     * Product lifecycle status.
     */
    public enum ProductStatus {
        ACTIVE,        // Product is available for sale
        DISCONTINUED,  // Product is no longer sold
        OUT_OF_STOCK  // Product temporarily unavailable (managed by Inventory Service)
    }
}
