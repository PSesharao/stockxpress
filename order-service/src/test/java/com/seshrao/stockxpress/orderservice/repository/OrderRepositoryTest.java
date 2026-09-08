package com.seshrao.stockxpress.orderservice.repository;

import com.seshrao.stockxpress.orderservice.model.Order;
import com.seshrao.stockxpress.orderservice.model.OrderLineItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrderRepository using @DataJpaTest.
 * Tests JPA repository operations with in-memory H2 database.
 * Covers CRUD operations, cascading, and relationship handling.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Order testOrder;
    private OrderLineItem lineItem1;
    private OrderLineItem lineItem2;

    @BeforeEach
    void setUp() {
        // Given - Setup test data
        lineItem1 = OrderLineItem.builder()
                .skuCode("iphone-13")
                .price(new BigDecimal("999.99"))
                .quantity(2)
                .build();

        lineItem2 = OrderLineItem.builder()
                .skuCode("macbook-pro")
                .price(new BigDecimal("2499.99"))
                .quantity(1)
                .build();

        testOrder = Order.builder()
                .orderNumber("ORD-12345")
                .orderLineItemList(Arrays.asList(lineItem1, lineItem2))
                .build();
    }

    /**
     * Test saving an order with multiple line items.
     * Given: Order with two line items
     * When: save is called
     * Then: Order and line items are persisted with generated IDs
     */
    @Test
    @DisplayName("Should save order with multiple line items successfully")
    void shouldSaveOrderWithMultipleLineItems() {
        // When
        Order savedOrder = orderRepository.save(testOrder);

        // Then
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getOrderNumber()).isEqualTo("ORD-12345");
        assertThat(savedOrder.getOrderLineItemList()).hasSize(2);
        assertThat(savedOrder.getOrderLineItemList().get(0).getId()).isNotNull();
        assertThat(savedOrder.getOrderLineItemList().get(1).getId()).isNotNull();
        assertThat(savedOrder.getOrderLineItemList().get(0).getSkuCode()).isEqualTo("iphone-13");
        assertThat(savedOrder.getOrderLineItemList().get(1).getSkuCode()).isEqualTo("macbook-pro");
    }

    /**
     * Test finding an order by ID.
     * Given: Saved order in database
     * When: findById is called with valid ID
     * Then: Order is retrieved with all line items
     */
    @Test
    @DisplayName("Should find order by ID with all line items")
    void shouldFindOrderByIdWithAllLineItems() {
        // Given
        Order savedOrder = entityManager.persistAndFlush(testOrder);
        Long orderId = savedOrder.getId();

        // When
        Optional<Order> foundOrder = orderRepository.findById(orderId);

        // Then
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getId()).isEqualTo(orderId);
        assertThat(foundOrder.get().getOrderNumber()).isEqualTo("ORD-12345");
        assertThat(foundOrder.get().getOrderLineItemList()).hasSize(2);
    }

    /**
     * Test finding order by non-existent ID.
     * Given: No order exists with given ID
     * When: findById is called with invalid ID
     * Then: Empty Optional is returned
     */
    @Test
    @DisplayName("Should return empty Optional when order not found by ID")
    void shouldReturnEmptyOptionalWhenOrderNotFound() {
        // Given
        Long nonExistentId = 999L;

        // When
        Optional<Order> foundOrder = orderRepository.findById(nonExistentId);

        // Then
        assertThat(foundOrder).isEmpty();
    }

    /**
     * Test saving order with single line item.
     * Given: Order with one line item
     * When: save is called
     * Then: Order and single line item are persisted
     */
    @Test
    @DisplayName("Should save order with single line item")
    void shouldSaveOrderWithSingleLineItem() {
        // Given
        testOrder.setOrderLineItemList(Collections.singletonList(lineItem1));

        // When
        Order savedOrder = orderRepository.save(testOrder);

        // Then
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getOrderLineItemList()).hasSize(1);
        assertThat(savedOrder.getOrderLineItemList().get(0).getSkuCode()).isEqualTo("iphone-13");
    }

    /**
     * Test saving order with empty line items list.
     * Given: Order with empty line items list
     * When: save is called
     * Then: Order is persisted with empty list
     */
    @Test
    @DisplayName("Should save order with empty line items list")
    void shouldSaveOrderWithEmptyLineItemsList() {
        // Given
        testOrder.setOrderLineItemList(Collections.emptyList());

        // When
        Order savedOrder = orderRepository.save(testOrder);

        // Then
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getOrderLineItemList()).isEmpty();
    }

    /**
     * Test cascade persist for line items.
     * Given: Order with line items not explicitly persisted
     * When: save is called on order
     * Then: Line items are automatically persisted due to CascadeType.ALL
     */
    @Test
    @DisplayName("Should cascade persist line items when saving order")
    void shouldCascadePersistLineItems() {
        // When
        Order savedOrder = orderRepository.save(testOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order foundOrder = entityManager.find(Order.class, savedOrder.getId());
        assertThat(foundOrder.getOrderLineItemList()).hasSize(2);
        assertThat(foundOrder.getOrderLineItemList().get(0).getId()).isNotNull();
        assertThat(foundOrder.getOrderLineItemList().get(1).getId()).isNotNull();
    }

    /**
     * Test deleting an order cascades to line items.
     * Given: Saved order with line items
     * When: delete is called on order
     * Then: Order and all line items are removed from database
     */
    @Test
    @DisplayName("Should cascade delete line items when deleting order")
    void shouldCascadeDeleteLineItems() {
        // Given
        Order savedOrder = orderRepository.save(testOrder);
        Long orderId = savedOrder.getId();
        Long lineItem1Id = savedOrder.getOrderLineItemList().get(0).getId();
        Long lineItem2Id = savedOrder.getOrderLineItemList().get(1).getId();
        entityManager.flush();

        // When
        orderRepository.deleteById(orderId);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order deletedOrder = entityManager.find(Order.class, orderId);
        OrderLineItem deletedLineItem1 = entityManager.find(OrderLineItem.class, lineItem1Id);
        OrderLineItem deletedLineItem2 = entityManager.find(OrderLineItem.class, lineItem2Id);
        
        assertThat(deletedOrder).isNull();
        assertThat(deletedLineItem1).isNull();
        assertThat(deletedLineItem2).isNull();
    }

    /**
     * Test finding all orders.
     * Given: Multiple orders in database
     * When: findAll is called
     * Then: All orders are retrieved
     */
    @Test
    @DisplayName("Should find all orders in database")
    void shouldFindAllOrders() {
        // Given
        Order order2 = Order.builder()
                .orderNumber("ORD-67890")
                .orderLineItemList(Collections.singletonList(
                        OrderLineItem.builder()
                                .skuCode("ipad-pro")
                                .price(new BigDecimal("799.99"))
                                .quantity(1)
                                .build()
                ))
                .build();
        
        orderRepository.save(testOrder);
        orderRepository.save(order2);
        entityManager.flush();

        // When
        List<Order> allOrders = orderRepository.findAll();

        // Then
        assertThat(allOrders).hasSize(2);
        assertThat(allOrders).extracting(Order::getOrderNumber)
                .containsExactlyInAnyOrder("ORD-12345", "ORD-67890");
    }

    /**
     * Test updating order line items.
     * Given: Saved order
     * When: Order line items are modified and saved
     * Then: Changes are persisted correctly
     */
    @Test
    @DisplayName("Should update order line items successfully")
    void shouldUpdateOrderLineItems() {
        // Given
        Order savedOrder = orderRepository.save(testOrder);
        Long orderId = savedOrder.getId();
        entityManager.flush();
        entityManager.clear();

        // When
        Order orderToUpdate = orderRepository.findById(orderId).get();
        orderToUpdate.getOrderLineItemList().get(0).setQuantity(5);
        orderRepository.save(orderToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order updatedOrder = orderRepository.findById(orderId).get();
        assertThat(updatedOrder.getOrderLineItemList().get(0).getQuantity()).isEqualTo(5);
    }

    /**
     * Test saving order with large decimal price.
     * Given: Order with very large price values
     * When: save is called
     * Then: Order is persisted with accurate decimal values
     */
    @Test
    @DisplayName("Should save order with large decimal prices accurately")
    void shouldSaveOrderWithLargeDecimalPrices() {
        // Given
        lineItem1.setPrice(new BigDecimal("999999.99"));
        lineItem2.setPrice(new BigDecimal("12345678.12"));

        // When
        Order savedOrder = orderRepository.save(testOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order foundOrder = orderRepository.findById(savedOrder.getId()).get();
        assertThat(foundOrder.getOrderLineItemList().get(0).getPrice())
                .isEqualByComparingTo(new BigDecimal("999999.99"));
        assertThat(foundOrder.getOrderLineItemList().get(1).getPrice())
                .isEqualByComparingTo(new BigDecimal("12345678.12"));
    }

    /**
     * Test saving order with special characters in order number.
     * Given: Order with special characters in order number
     * When: save is called
     * Then: Order is persisted with correct order number
     */
    @Test
    @DisplayName("Should save order with special characters in order number")
    void shouldSaveOrderWithSpecialCharactersInOrderNumber() {
        // Given
        testOrder.setOrderNumber("ORD-2024-09-08_@123");

        // When
        Order savedOrder = orderRepository.save(testOrder);

        // Then
        assertThat(savedOrder.getOrderNumber()).isEqualTo("ORD-2024-09-08_@123");
    }

    /**
     * Test saving order with UUID order number.
     * Given: Order with UUID as order number
     * When: save is called
     * Then: Order is persisted with UUID order number
     */
    @Test
    @DisplayName("Should save order with UUID order number")
    void shouldSaveOrderWithUuidOrderNumber() {
        // Given
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        testOrder.setOrderNumber(uuid);

        // When
        Order savedOrder = orderRepository.save(testOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order foundOrder = orderRepository.findById(savedOrder.getId()).get();
        assertThat(foundOrder.getOrderNumber()).isEqualTo(uuid);
    }

    /**
     * Test count operation.
     * Given: Multiple orders in database
     * When: count is called
     * Then: Correct count is returned
     */
    @Test
    @DisplayName("Should count orders correctly")
    void shouldCountOrdersCorrectly() {
        // Given
        orderRepository.save(testOrder);
        orderRepository.save(Order.builder()
                .orderNumber("ORD-99999")
                .orderLineItemList(Collections.emptyList())
                .build());
        entityManager.flush();

        // When
        long count = orderRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    /**
     * Test exists by ID operation.
     * Given: Saved order in database
     * When: existsById is called
     * Then: Returns true for existing ID and false for non-existing ID
     */
    @Test
    @DisplayName("Should check if order exists by ID")
    void shouldCheckIfOrderExistsById() {
        // Given
        Order savedOrder = orderRepository.save(testOrder);
        Long existingId = savedOrder.getId();
        Long nonExistingId = 999L;

        // When
        boolean exists = orderRepository.existsById(existingId);
        boolean notExists = orderRepository.existsById(nonExistingId);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
