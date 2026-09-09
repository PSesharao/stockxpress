package com.seshrao.stockxpress.inventoryservice.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Inventory domain model.
 * Tests business logic for stock management, reservations, and invariants.
 */
@DisplayName("Inventory Domain Model Tests")
class InventoryTest {

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("create() Factory Method")
    class CreateFactoryTests {

        @Test
        @DisplayName("Should create inventory with valid parameters")
        void shouldCreateInventoryWithValidParameters() {
            // Act
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Assert
            assertThat(inventory).isNotNull();
            assertThat(inventory.getSkuCode()).isEqualTo("IPHONE_13");
            assertThat(inventory.getQuantity()).isEqualTo(100);
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            assertThat(inventory.getLastRestockedDate()).isNotNull();
            assertThat(inventory.getMinimumStockLevel()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should create inventory with zero initial quantity")
        void shouldCreateInventoryWithZeroInitialQuantity() {
            // Act
            Inventory inventory = Inventory.create("OUT_OF_STOCK_ITEM", 0);

            // Assert
            assertThat(inventory.getQuantity()).isEqualTo(0);
            assertThat(inventory.isInStock()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid-sku", "lowercase", "with spaces"})
        @DisplayName("Should fail with invalid SKU format")
        void shouldFailWithInvalidSkuFormat(String invalidSku) {
            // Act & Assert
            assertThatThrownBy(() -> Inventory.create(invalidSku, 100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid SKU code format");
        }

        @Test
        @DisplayName("Should fail when quantity is negative")
        void shouldFailWhenQuantityIsNegative() {
            // Act & Assert
            assertThatThrownBy(() -> Inventory.create("VALID_SKU", -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity must be non-negative");
        }
    }

    // ==================== Stock Availability Tests ====================

    @Nested
    @DisplayName("Stock Availability Checks")
    class StockAvailabilityTests {

        @Test
        @DisplayName("isInStock() should return true when stock available")
        void isInStockShouldReturnTrueWhenStockAvailable() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Act & Assert
            assertThat(inventory.isInStock()).isTrue();
        }

        @Test
        @DisplayName("isInStock() should return false when out of stock")
        void isInStockShouldReturnFalseWhenOutOfStock() {
            // Arrange
            Inventory inventory = Inventory.create("OUT_OF_STOCK", 0);

            // Act & Assert
            assertThat(inventory.isInStock()).isFalse();
        }

        @Test
        @DisplayName("isInStock() should consider reserved quantity")
        void isInStockShouldConsiderReservedQuantity() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 10);
            inventory.reserveStock(10);

            // Act & Assert
            assertThat(inventory.isInStock()).isFalse();
        }

        @Test
        @DisplayName("hasStock() should return true when sufficient stock")
        void hasStockShouldReturnTrueWhenSufficientStock() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Act & Assert
            assertThat(inventory.hasStock(50)).isTrue();
            assertThat(inventory.hasStock(100)).isTrue();
        }

        @Test
        @DisplayName("hasStock() should return false when insufficient stock")
        void hasStockShouldReturnFalseWhenInsufficientStock() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Act & Assert
            assertThat(inventory.hasStock(101)).isFalse();
        }

        @Test
        @DisplayName("getAvailableQuantity() should exclude reserved stock")
        void getAvailableQuantityShouldExcludeReservedStock() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);
            inventory.reserveStock(30);

            // Act
            Integer available = inventory.getAvailableQuantity();

            // Assert
            assertThat(available).isEqualTo(70);
        }
    }

    // ==================== Stock Reservation Tests ====================

    @Nested
    @DisplayName("Stock Reservation Logic")
    class StockReservationTests {

        @Test
        @DisplayName("Should reserve stock successfully")
        void shouldReserveStockSuccessfully() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Act
            inventory.reserveStock(30);

            // Assert
            assertThat(inventory.getReservedQuantity()).isEqualTo(30);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("Should reserve multiple times cumulatively")
        void shouldReserveMultipleTimesCumulatively() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Act
            inventory.reserveStock(20);
            inventory.reserveStock(30);

            // Assert
            assertThat(inventory.getReservedQuantity()).isEqualTo(50);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should fail to reserve more than available")
        void shouldFailToReserveMoreThanAvailable() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Act & Assert
            assertThatThrownBy(() -> inventory.reserveStock(101))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("Should fail to reserve negative quantity")
        void shouldFailToReserveNegativeQuantity() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Act & Assert
            assertThatThrownBy(() -> inventory.reserveStock(-10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot reserve negative quantity");
        }
    }

    // ==================== Stock Release Tests ====================

    @Nested
    @DisplayName("Stock Release Logic")
    class StockReleaseTests {

        @Test
        @DisplayName("Should release reserved stock successfully")
        void shouldReleaseReservedStockSuccessfully() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);
            inventory.reserveStock(50);

            // Act
            inventory.releaseStock(20);

            // Assert
            assertThat(inventory.getReservedQuantity()).isEqualTo(30);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("Should fail to release more than reserved")
        void shouldFailToReleaseMoreThanReserved() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);
            inventory.reserveStock(20);

            // Act & Assert
            assertThatThrownBy(() -> inventory.releaseStock(30))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot release");
        }

        @Test
        @DisplayName("Should fail to release negative quantity")
        void shouldFailToReleaseNegativeQuantity() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Act & Assert
            assertThatThrownBy(() -> inventory.releaseStock(-10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot release negative quantity");
        }
    }

    // ==================== Stock Decrement Tests ====================

    @Nested
    @DisplayName("Stock Decrement Logic")
    class StockDecrementTests {

        @Test
        @DisplayName("Should decrement stock successfully")
        void shouldDecrementStockSuccessfully() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);
            inventory.reserveStock(20);

            // Act
            inventory.decrementStock(20);

            // Assert
            assertThat(inventory.getQuantity()).isEqualTo(80);
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should fail to decrement below zero")
        void shouldFailToDecrementBelowZero() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 50);

            // Act & Assert
            assertThatThrownBy(() -> inventory.decrementStock(60))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot decrement");
        }
    }

    // ==================== Restock Tests ====================

    @Nested
    @DisplayName("Restock Logic")
    class RestockTests {

        @Test
        @DisplayName("Should restock successfully")
        void shouldRestockSuccessfully() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 50);
            LocalDateTime beforeRestock = inventory.getLastRestockedDate();

            // Act
            inventory.restock(100);

            // Assert
            assertThat(inventory.getQuantity()).isEqualTo(150);
            assertThat(inventory.getLastRestockedDate()).isAfter(beforeRestock);
        }

        @Test
        @DisplayName("Should fail to restock zero or negative")
        void shouldFailToRestockZeroOrNegative() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 50);

            // Act & Assert
            assertThatThrownBy(() -> inventory.restock(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Restock quantity must be positive");

            assertThatThrownBy(() -> inventory.restock(-10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Restock quantity must be positive");
        }
    }

    // ==================== Low Stock Detection Tests ====================

    @Nested
    @DisplayName("Low Stock Detection")
    class LowStockTests {

        @Test
        @DisplayName("isLowStock() should return true when below threshold")
        void isLowStockShouldReturnTrueWhenBelowThreshold() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 5);

            // Act & Assert
            assertThat(inventory.isLowStock()).isTrue();
        }

        @Test
        @DisplayName("isLowStock() should return false when above threshold")
        void isLowStockShouldReturnFalseWhenAboveThreshold() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 50);

            // Act & Assert
            assertThat(inventory.isLowStock()).isFalse();
        }

        @Test
        @DisplayName("Should allow custom minimum stock level")
        void shouldAllowCustomMinimumStockLevel() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 25);

            // Act
            inventory.setMinimumStockLevel(30);

            // Assert
            assertThat(inventory.isLowStock()).isTrue();
        }
    }

    // ==================== Inventory Reconciliation Tests ====================

    @Nested
    @DisplayName("Inventory Reconciliation")
    class ReconciliationTests {

        @Test
        @DisplayName("Should reconcile inventory to physical count")
        void shouldReconcileInventoryToPhysicalCount() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);

            // Act
            inventory.reconcileInventory(95);

            // Assert
            assertThat(inventory.getQuantity()).isEqualTo(95);
        }

        @Test
        @DisplayName("Should fail if physical count less than reserved")
        void shouldFailIfPhysicalCountLessThanReserved() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);
            inventory.reserveStock(50);

            // Act & Assert
            assertThatThrownBy(() -> inventory.reconcileInventory(40))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Physical count");
        }

        @Test
        @DisplayName("Should allow physical count equal to reserved")
        void shouldAllowPhysicalCountEqualToReserved() {
            // Arrange
            Inventory inventory = Inventory.create("IPHONE_13", 100);
            inventory.reserveStock(50);

            // Act
            inventory.reconcileInventory(50);

            // Assert
            assertThat(inventory.getQuantity()).isEqualTo(50);
        }
    }

    // ==================== Equality Tests ====================

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {

        @Test
        @DisplayName("Inventories with same SKU should be equal")
        void inventoriesWithSameSkuShouldBeEqual() {
            // Arrange
            Inventory inv1 = Inventory.create("IPHONE_13", 100);
            Inventory inv2 = Inventory.create("IPHONE_13", 200);

            // Act & Assert
            assertThat(inv1).isEqualTo(inv2);
            assertThat(inv1.hashCode()).isEqualTo(inv2.hashCode());
        }

        @Test
        @DisplayName("Inventories with different SKUs should not be equal")
        void inventoriesWithDifferentSkusShouldNotBeEqual() {
            // Arrange
            Inventory inv1 = Inventory.create("IPHONE_13", 100);
            Inventory inv2 = Inventory.create("MACBOOK_PRO", 100);

            // Act & Assert
            assertThat(inv1).isNotEqualTo(inv2);
        }
    }
}
