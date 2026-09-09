package com.seshrao.stockxpress.productservice.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Product domain model.
 * Tests business logic for pricing, lifecycle, and invariants.
 */
@DisplayName("Product Domain Model Tests")
class ProductTest {

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("create() Factory Method")
    class CreateFactoryTests {

        @Test
        @DisplayName("Should create product with valid parameters")
        void shouldCreateProductWithValidParameters() {
            // Act
            Product product = Product.create(
                    "iPhone 13 Pro",
                    "Latest Apple smartphone with A15 chip",
                    new BigDecimal("999.99"),
                    "IPHONE_13_PRO",
                    "Electronics"
            );

            // Assert
            assertThat(product).isNotNull();
            assertThat(product.getName()).isEqualTo("iPhone 13 Pro");
            assertThat(product.getDescription()).isEqualTo("Latest Apple smartphone with A15 chip");
            assertThat(product.getPrice()).isEqualByComparingTo("999.99");
            assertThat(product.getSkuCode()).isEqualTo("IPHONE_13_PRO");
            assertThat(product.getCategory()).isEqualTo("Electronics");
            assertThat(product.getStatus()).isEqualTo(Product.ProductStatus.ACTIVE);
            assertThat(product.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("Should normalize SKU code to uppercase")
        void shouldNormalizeSkuCodeToUppercase() {
            // Act
            Product product = Product.create(
                    "Test Product",
                    "Description",
                    new BigDecimal("99.99"),
                    "test_sku_123",
                    "Category"
            );

            // Assert
            assertThat(product.getSkuCode()).isEqualTo("TEST_SKU_123");
        }

        @Test
        @DisplayName("Should trim whitespace from inputs")
        void shouldTrimWhitespaceFromInputs() {
            // Act
            Product product = Product.create(
                    "  Product Name  ",
                    "  Description  ",
                    new BigDecimal("99.99"),
                    "SKU_CODE",
                    "  Category  "
            );

            // Assert
            assertThat(product.getName()).isEqualTo("Product Name");
            assertThat(product.getDescription()).isEqualTo("Description");
            assertThat(product.getCategory()).isEqualTo("Category");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "AB"})
        @DisplayName("Should fail with invalid product name")
        void shouldFailWithInvalidProductName(String invalidName) {
            // Act & Assert
            assertThatThrownBy(() -> Product.create(
                    invalidName,
                    "Description",
                    new BigDecimal("99.99"),
                    "SKU_CODE",
                    "Category"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name");
        }

        @Test
        @DisplayName("Should fail when name is null")
        void shouldFailWhenNameIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> Product.create(
                    null,
                    "Description",
                    new BigDecimal("99.99"),
                    "SKU_CODE",
                    "Category"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name cannot be null");
        }

        @Test
        @DisplayName("Should fail when price is null")
        void shouldFailWhenPriceIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> Product.create(
                    "Product Name",
                    "Description",
                    null,
                    "SKU_CODE",
                    "Category"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price cannot be null");
        }

        @Test
        @DisplayName("Should fail when price is negative")
        void shouldFailWhenPriceIsNegative() {
            // Act & Assert
            assertThatThrownBy(() -> Product.create(
                    "Product Name",
                    "Description",
                    new BigDecimal("-10.00"),
                    "SKU_CODE",
                    "Category"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be non-negative");
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid-sku", "lower case", "special@char"})
        @DisplayName("Should fail with invalid SKU format")
        void shouldFailWithInvalidSkuFormat(String invalidSku) {
            // Act & Assert
            assertThatThrownBy(() -> Product.create(
                    "Product Name",
                    "Description",
                    new BigDecimal("99.99"),
                    invalidSku,
                    "Category"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid SKU code format");
        }

        @Test
        @DisplayName("Should fail when category is empty")
        void shouldFailWhenCategoryIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> Product.create(
                    "Product Name",
                    "Description",
                    new BigDecimal("99.99"),
                    "SKU_CODE",
                    ""
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category cannot be null or empty");
        }
    }

    // ==================== Price Management Tests ====================

    @Nested
    @DisplayName("Price Management")
    class PriceManagementTests {

        @Test
        @DisplayName("Should update price successfully")
        void shouldUpdatePriceSuccessfully() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );
            LocalDateTime beforeUpdate = LocalDateTime.now();

            // Act
            product.updatePrice(new BigDecimal("899.99"));

            // Assert
            assertThat(product.getPrice()).isEqualByComparingTo("899.99");
            assertThat(product.getPreviousPrice()).isEqualByComparingTo("999.99");
            assertThat(product.getPriceChangedDate()).isAfter(beforeUpdate);
            assertThat(product.getLastModifiedDate()).isNotNull();
        }

        @Test
        @DisplayName("Should not update when price is same")
        void shouldNotUpdateWhenPriceIsSame() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act
            product.updatePrice(new BigDecimal("999.99"));

            // Assert
            assertThat(product.getPreviousPrice()).isNull();
            assertThat(product.getPriceChangedDate()).isNull();
        }

        @Test
        @DisplayName("hasPriceIncreased() should return true when price increased")
        void hasPriceIncreasedShouldReturnTrueWhenPriceIncreased() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act
            product.updatePrice(new BigDecimal("1099.99"));

            // Assert
            assertThat(product.hasPriceIncreased()).isTrue();
        }

        @Test
        @DisplayName("isOnSale() should return true when price decreased")
        void isOnSaleShouldReturnTrueWhenPriceDecreased() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act
            product.updatePrice(new BigDecimal("799.99"));

            // Assert
            assertThat(product.isOnSale()).isTrue();
        }

        @Test
        @DisplayName("Should calculate discount percentage correctly")
        void shouldCalculateDiscountPercentageCorrectly() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("1000.00"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act
            product.updatePrice(new BigDecimal("800.00"));
            BigDecimal discount = product.getDiscountPercentage();

            // Assert
            assertThat(discount).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("Should return zero discount when not on sale")
        void shouldReturnZeroDiscountWhenNotOnSale() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act
            BigDecimal discount = product.getDiscountPercentage();

            // Assert
            assertThat(discount).isEqualByComparingTo("0.00");
        }
    }

    // ==================== Product Lifecycle Tests ====================

    @Nested
    @DisplayName("Product Lifecycle Management")
    class LifecycleTests {

        @Test
        @DisplayName("Should discontinue active product")
        void shouldDiscontinueActiveProduct() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act
            product.discontinue();

            // Assert
            assertThat(product.getStatus()).isEqualTo(Product.ProductStatus.DISCONTINUED);
            assertThat(product.getLastModifiedDate()).isNotNull();
        }

        @Test
        @DisplayName("Should fail to discontinue already discontinued product")
        void shouldFailToDiscontinueAlreadyDiscontinuedProduct() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );
            product.discontinue();

            // Act & Assert
            assertThatThrownBy(product::discontinue)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already discontinued");
        }

        @Test
        @DisplayName("Should reactivate discontinued product")
        void shouldReactivateDiscontinuedProduct() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );
            product.discontinue();

            // Act
            product.reactivate();

            // Assert
            assertThat(product.getStatus()).isEqualTo(Product.ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should fail to reactivate already active product")
        void shouldFailToReactivateAlreadyActiveProduct() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act & Assert
            assertThatThrownBy(product::reactivate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already active");
        }

        @Test
        @DisplayName("isAvailableForOrder() should return true for active products")
        void isAvailableForOrderShouldReturnTrueForActiveProducts() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act & Assert
            assertThat(product.isAvailableForOrder()).isTrue();
        }

        @Test
        @DisplayName("isAvailableForOrder() should return false for discontinued products")
        void isAvailableForOrderShouldReturnFalseForDiscontinuedProducts() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );
            product.discontinue();

            // Act & Assert
            assertThat(product.isAvailableForOrder()).isFalse();
        }
    }

    // ==================== Product Information Update Tests ====================

    @Nested
    @DisplayName("Product Information Updates")
    class InformationUpdateTests {

        @Test
        @DisplayName("Should update product information")
        void shouldUpdateProductInformation() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Old description",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act
            product.updateInfo(
                    "iPhone 13 Pro Max",
                    "Updated description with new features",
                    "Mobile Devices"
            );

            // Assert
            assertThat(product.getName()).isEqualTo("iPhone 13 Pro Max");
            assertThat(product.getDescription()).isEqualTo("Updated description with new features");
            assertThat(product.getCategory()).isEqualTo("Mobile Devices");
            assertThat(product.getLastModifiedDate()).isNotNull();
        }

        @Test
        @DisplayName("Should allow partial updates")
        void shouldAllowPartialUpdates() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Description",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act
            product.updateInfo("New Name", null, null);

            // Assert
            assertThat(product.getName()).isEqualTo("New Name");
            assertThat(product.getDescription()).isEqualTo("Description");
            assertThat(product.getCategory()).isEqualTo("Electronics");
        }
    }

    // ==================== Business Query Tests ====================

    @Nested
    @DisplayName("Business Query Methods")
    class BusinessQueryTests {

        @Test
        @DisplayName("isNew() should return true for recently created products")
        void isNewShouldReturnTrueForRecentlyCreatedProducts() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act & Assert
            assertThat(product.isNew()).isTrue();
        }

        @Test
        @DisplayName("isPriceRecentlyChanged() should detect recent price changes")
        void isPriceRecentlyChangedShouldDetectRecentPriceChanges() {
            // Arrange
            Product product = Product.create(
                    "iPhone 13",
                    "Smartphone",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );

            // Act
            product.updatePrice(new BigDecimal("899.99"));

            // Assert
            assertThat(product.isPriceRecentlyChanged(7)).isTrue();
        }
    }

    // ==================== Equality Tests ====================

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {

        @Test
        @DisplayName("Products with same SKU should be equal")
        void productsWithSameSkuShouldBeEqual() {
            // Arrange
            Product product1 = Product.create(
                    "iPhone 13",
                    "Description 1",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );
            Product product2 = Product.create(
                    "iPhone 13 Pro",
                    "Description 2",
                    new BigDecimal("1099.99"),
                    "IPHONE_13",
                    "Mobile"
            );

            // Act & Assert
            assertThat(product1).isEqualTo(product2);
            assertThat(product1.hashCode()).isEqualTo(product2.hashCode());
        }

        @Test
        @DisplayName("Products with different SKUs should not be equal")
        void productsWithDifferentSkusShouldNotBeEqual() {
            // Arrange
            Product product1 = Product.create(
                    "iPhone 13",
                    "Description",
                    new BigDecimal("999.99"),
                    "IPHONE_13",
                    "Electronics"
            );
            Product product2 = Product.create(
                    "iPhone 13",
                    "Description",
                    new BigDecimal("999.99"),
                    "IPHONE_14",
                    "Electronics"
            );

            // Act & Assert
            assertThat(product1).isNotEqualTo(product2);
        }
    }
}
