package com.seshrao.stockxpress.orderservice.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OrderLineItem value object.
 * Tests business logic, validation, and invariants.
 */
@DisplayName("OrderLineItem Value Object Tests")
class OrderLineItemTest {

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("create() Factory Method")
    class CreateFactoryTests {

        @Test
        @DisplayName("Should create line item with valid parameters")
        void shouldCreateLineItemWithValidParameters() {
            // Act
            OrderLineItem lineItem = OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    2
            );

            // Assert
            assertThat(lineItem).isNotNull();
            assertThat(lineItem.getSkuCode()).isEqualTo("IPHONE_13");
            assertThat(lineItem.getPrice()).isEqualByComparingTo("999.99");
            assertThat(lineItem.getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should fail when SKU code is null")
        void shouldFailWhenSkuCodeIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> OrderLineItem.create(
                    null,
                    new BigDecimal("999.99"),
                    2
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SKU code cannot be null");
        }

        @Test
        @DisplayName("Should fail when SKU code is empty")
        void shouldFailWhenSkuCodeIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> OrderLineItem.create(
                    "",
                    new BigDecimal("999.99"),
                    2
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SKU code cannot be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid-sku", "lower_case", "with spaces", "special@char"})
        @DisplayName("Should fail with invalid SKU format")
        void shouldFailWithInvalidSkuFormat(String invalidSku) {
            // Act & Assert
            assertThatThrownBy(() -> OrderLineItem.create(
                    invalidSku,
                    new BigDecimal("999.99"),
                    2
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid SKU code format");
        }

        @Test
        @DisplayName("Should fail when price is null")
        void shouldFailWhenPriceIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> OrderLineItem.create(
                    "IPHONE_13",
                    null,
                    2
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price cannot be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1", "-999.99"})
        @DisplayName("Should fail when price is zero or negative")
        void shouldFailWhenPriceIsZeroOrNegative(String priceValue) {
            // Act & Assert
            assertThatThrownBy(() -> OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal(priceValue),
                    2
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be positive");
        }

        @Test
        @DisplayName("Should fail when quantity is null")
        void shouldFailWhenQuantityIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity cannot be null");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -10})
        @DisplayName("Should fail when quantity is zero or negative")
        void shouldFailWhenQuantityIsZeroOrNegative(int quantity) {
            // Act & Assert
            assertThatThrownBy(() -> OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    quantity
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity must be at least 1");
        }
    }

    // ==================== Business Logic Tests ====================

    @Nested
    @DisplayName("getLineTotal() Calculation")
    class LineTotalTests {

        @Test
        @DisplayName("Should calculate line total correctly")
        void shouldCalculateLineTotalCorrectly() {
            // Arrange
            OrderLineItem lineItem = OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    2
            );

            // Act
            BigDecimal lineTotal = lineItem.getLineTotal();

            // Assert
            assertThat(lineTotal).isEqualByComparingTo("1999.98");
        }

        @Test
        @DisplayName("Should handle single quantity")
        void shouldHandleSingleQuantity() {
            // Arrange
            OrderLineItem lineItem = OrderLineItem.create(
                    "MACBOOK_PRO",
                    new BigDecimal("2499.99"),
                    1
            );

            // Act
            BigDecimal lineTotal = lineItem.getLineTotal();

            // Assert
            assertThat(lineTotal).isEqualByComparingTo("2499.99");
        }
    }

    @Nested
    @DisplayName("Quantity Management")
    class QuantityManagementTests {

        @Test
        @DisplayName("Should increase quantity correctly")
        void shouldIncreaseQuantityCorrectly() {
            // Arrange
            OrderLineItem lineItem = OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    2
            );

            // Act
            lineItem.increaseQuantity(3);

            // Assert
            assertThat(lineItem.getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should fail to increase by negative amount")
        void shouldFailToIncreaseByNegativeAmount() {
            // Arrange
            OrderLineItem lineItem = OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    2
            );

            // Act & Assert
            assertThatThrownBy(() -> lineItem.increaseQuantity(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot increase quantity by negative amount");
        }

        @Test
        @DisplayName("Should decrease quantity correctly")
        void shouldDecreaseQuantityCorrectly() {
            // Arrange
            OrderLineItem lineItem = OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    5
            );

            // Act
            lineItem.decreaseQuantity(2);

            // Assert
            assertThat(lineItem.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should fail to decrease below 1")
        void shouldFailToDecreaseBelowOne() {
            // Arrange
            OrderLineItem lineItem = OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    2
            );

            // Act & Assert
            assertThatThrownBy(() -> lineItem.decreaseQuantity(2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot reduce quantity below 1");
        }
    }

    // ==================== Equality Tests ====================

    @Nested
    @DisplayName("Value Object Equality")
    class EqualityTests {

        @Test
        @DisplayName("Line items with same values should be equal")
        void lineItemsWithSameValuesShouldBeEqual() {
            // Arrange
            OrderLineItem lineItem1 = OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    2
            );
            OrderLineItem lineItem2 = OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    2
            );

            // Act & Assert
            assertThat(lineItem1).isEqualTo(lineItem2);
            assertThat(lineItem1.hashCode()).isEqualTo(lineItem2.hashCode());
        }

        @Test
        @DisplayName("Line items with different SKUs should not be equal")
        void lineItemsWithDifferentSkusShouldNotBeEqual() {
            // Arrange
            OrderLineItem lineItem1 = OrderLineItem.create(
                    "IPHONE_13",
                    new BigDecimal("999.99"),
                    2
            );
            OrderLineItem lineItem2 = OrderLineItem.create(
                    "MACBOOK_PRO",
                    new BigDecimal("999.99"),
                    2
            );

            // Act & Assert
            assertThat(lineItem1).isNotEqualTo(lineItem2);
        }
    }
}
