package com.seshrao.stockxpress.orderservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Order domain model.
 * Tests business logic, invariants, and state transitions.
 * Follows AAA pattern (Arrange, Act, Assert).
 */
@DisplayName("Order Domain Model Tests")
class OrderTest {

    private List<OrderLineItem> validLineItems;
    private OrderLineItem lineItem1;
    private OrderLineItem lineItem2;

    @BeforeEach
    void setUp() {
        lineItem1 = OrderLineItem.create("IPHONE_13", new BigDecimal("999.99"), 2);
        lineItem2 = OrderLineItem.create("MACBOOK_PRO", new BigDecimal("2499.99"), 1);
        validLineItems = Arrays.asList(lineItem1, lineItem2);
    }

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("createNewOrder() Factory Method")
    class CreateNewOrderTests {

        @Test
        @DisplayName("Should create order with valid line items")
        void shouldCreateOrderWithValidLineItems() {
            // Act
            Order order = Order.createNewOrder(validLineItems);

            // Assert
            assertThat(order).isNotNull();
            assertThat(order.getOrderNumber()).isNotNull()
                    .startsWith("ORD-")
                    .matches("ORD-\\d{8}-[A-Z0-9]{8}");
            assertThat(order.getOrderLineItemList()).hasSize(2);
            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
            assertThat(order.getOrderDate()).isNotNull();
            assertThat(order.getTotalAmount()).isEqualByComparingTo("3499.97");
        }

        @Test
        @DisplayName("Should fail when line items are null")
        void shouldFailWhenLineItemsAreNull() {
            // Act & Assert
            assertThatThrownBy(() -> Order.createNewOrder(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot create order without line items");
        }

        @Test
        @DisplayName("Should fail when line items are empty")
        void shouldFailWhenLineItemsAreEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> Order.createNewOrder(Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot create order without line items");
        }

        @Test
        @DisplayName("Should generate unique order numbers")
        void shouldGenerateUniqueOrderNumbers() {
            // Act
            Order order1 = Order.createNewOrder(validLineItems);
            Order order2 = Order.createNewOrder(validLineItems);

            // Assert
            assertThat(order1.getOrderNumber()).isNotEqualTo(order2.getOrderNumber());
        }
    }

    // ==================== Business Logic Tests ====================

    @Nested
    @DisplayName("calculateTotal() Business Logic")
    class CalculateTotalTests {

        @Test
        @DisplayName("Should calculate total from line items")
        void shouldCalculateTotalFromLineItems() {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);

            // Act
            order.calculateTotal();

            // Assert
            // 999.99 * 2 + 2499.99 * 1 = 1999.98 + 2499.99 = 4499.97
            assertThat(order.getTotalAmount()).isEqualByComparingTo("3499.97");
        }

        @Test
        @DisplayName("Should recalculate total after adding items")
        void shouldRecalculateTotalAfterAddingItems() {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);
            BigDecimal initialTotal = order.getTotalAmount();

            // Act
            OrderLineItem newItem = OrderLineItem.create("AIRPODS", new BigDecimal("199.99"), 1);
            order.getOrderLineItemList().add(newItem);
            order.calculateTotal();

            // Assert
            assertThat(order.getTotalAmount()).isGreaterThan(initialTotal);
            assertThat(order.getTotalAmount()).isEqualByComparingTo("3699.96");
        }
    }

    @Nested
    @DisplayName("State Transition Tests")
    class StateTransitionTests {

        @Test
        @DisplayName("Should confirm pending order")
        void shouldConfirmPendingOrder() {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);
            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

            // Act
            order.confirm();

            // Assert
            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should fail to confirm already confirmed order")
        void shouldFailToConfirmAlreadyConfirmedOrder() {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);
            order.confirm();

            // Act & Assert
            assertThatThrownBy(order::confirm)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot confirm order");
        }

        @Test
        @DisplayName("Should cancel pending order")
        void shouldCancelPendingOrder() {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);

            // Act
            order.cancel();

            // Assert
            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should cancel confirmed order")
        void shouldCancelConfirmedOrder() {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);
            order.confirm();

            // Act
            order.cancel();

            // Assert
            assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        }

        @ParameterizedTest
        @EnumSource(value = Order.OrderStatus.class, names = {"SHIPPED", "DELIVERED"})
        @DisplayName("Should fail to cancel shipped or delivered orders")
        void shouldFailToCancelShippedOrDeliveredOrders(Order.OrderStatus status) {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);
            // Use reflection to set status (for testing purposes)
            setOrderStatus(order, status);

            // Act & Assert
            assertThatThrownBy(order::cancel)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel order");
        }
    }

    @Nested
    @DisplayName("Business Query Methods")
    class BusinessQueryTests {

        @Test
        @DisplayName("canBePlaced() should return true for valid order")
        void canBePlacedShouldReturnTrueForValidOrder() {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);

            // Act & Assert
            assertThat(order.canBePlaced()).isTrue();
        }

        @Test
        @DisplayName("getItemCount() should return correct count")
        void getItemCountShouldReturnCorrectCount() {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);

            // Act & Assert
            assertThat(order.getItemCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("containsSku() should find existing SKU")
        void containsSkuShouldFindExistingSku() {
            // Arrange
            Order order = Order.createNewOrder(validLineItems);

            // Act & Assert
            assertThat(order.containsSku("IPHONE_13")).isTrue();
            assertThat(order.containsSku("MACBOOK_PRO")).isTrue();
            assertThat(order.containsSku("NONEXISTENT")).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {

        @Test
        @DisplayName("Orders with same order number should be equal")
        void ordersWithSameOrderNumberShouldBeEqual() {
            // Arrange
            Order order1 = Order.createNewOrder(validLineItems);
            Order order2 = Order.createNewOrder(validLineItems);

            // Force same order number for testing
            setOrderNumber(order2, order1.getOrderNumber());

            // Act & Assert
            assertThat(order1).isEqualTo(order2);
            assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
        }

        @Test
        @DisplayName("Orders with different order numbers should not be equal")
        void ordersWithDifferentOrderNumbersShouldNotBeEqual() {
            // Arrange
            Order order1 = Order.createNewOrder(validLineItems);
            Order order2 = Order.createNewOrder(validLineItems);

            // Act & Assert
            assertThat(order1).isNotEqualTo(order2);
        }
    }

    // ==================== Helper Methods ====================

    private void setOrderStatus(Order order, Order.OrderStatus status) {
        try {
            var field = Order.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(order, status);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set order status", e);
        }
    }

    private void setOrderNumber(Order order, String orderNumber) {
        try {
            var field = Order.class.getDeclaredField("orderNumber");
            field.setAccessible(true);
            field.set(order, orderNumber);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set order number", e);
        }
    }
}
