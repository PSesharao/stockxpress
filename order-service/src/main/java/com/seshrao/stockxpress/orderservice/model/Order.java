package com.seshrao.stockxpress.orderservice.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order Domain Model - Rich Domain Model (DDD)
 * <p>
 * Encapsulates order business logic and invariants:
 * - Order must have at least one line item
 * - Order number is auto-generated and immutable
 * - Total calculation is encapsulated
 * - State transitions are controlled
 * <p>
 * Business Rules Enforced:
 * 1. Cannot create empty order
 * 2. Cannot modify order after placement
 * 3. Order number follows format: ORD-YYYYMMDD-UUID
 * 4. Order total is calculated from line items
 */
@Entity
@Table(name = "t_orders", indexes = {
        @Index(name = "idx_order_number", columnList = "order_number", unique = true),
        @Index(name = "idx_order_date", columnList = "order_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requirement
@ToString(exclude = "orderLineItemList") // Avoid circular references
@EqualsAndHashCode(of = "orderNumber") // Business key equality
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    @NotEmpty(message = "Order must have at least one line item")
    private List<OrderLineItem> orderLineItemList = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    // ==================== Factory Methods (Business Logic) ====================

    /**
     * Factory method to create a new order with validation.
     * Enforces business rule: Order must have at least one item.
     *
     * @param orderLineItems List of line items
     * @return New Order instance
     * @throws IllegalArgumentException if line items are null or empty
     */
    public static Order createNewOrder(List<OrderLineItem> orderLineItems) {
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot create order without line items");
        }

        Order order = new Order();
        order.orderNumber = generateOrderNumber();
        order.orderDate = LocalDateTime.now();
        order.status = OrderStatus.PENDING;
        order.orderLineItemList = new ArrayList<>(orderLineItems);
        order.calculateTotal();

        return order;
    }

    /**
     * Generates unique order number following format: ORD-YYYYMMDD-UUID
     * Business rule: Order numbers must be unique and traceable.
     */
    private static String generateOrderNumber() {
        String datePart = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                .format(LocalDateTime.now());
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%s", datePart, uniquePart);
    }

    // ==================== Business Logic Methods ====================

    /**
     * Calculates total order amount from line items.
     * Business rule: Total is sum of (price * quantity) for all items.
     */
    public void calculateTotal() {
        this.totalAmount = orderLineItemList.stream()
                .map(OrderLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Confirms the order (state transition).
     * Business rule: Can only confirm pending orders.
     *
     * @throws IllegalStateException if order is not in PENDING status
     */
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("Cannot confirm order %s in status %s", orderNumber, status)
            );
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * Cancels the order (state transition).
     * Business rule: Can only cancel pending or confirmed orders.
     *
     * @throws IllegalStateException if order is already shipped or delivered
     */
    public void cancel() {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    String.format("Cannot cancel order %s in status %s", orderNumber, status)
            );
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * Checks if order can be placed (all invariants satisfied).
     * Business rule: Order must have items and valid total.
     *
     * @return true if order is valid for placement
     */
    public boolean canBePlaced() {
        return orderLineItemList != null
                && !orderLineItemList.isEmpty()
                && totalAmount != null
                && totalAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Gets count of items in order.
     *
     * @return Total number of line items
     */
    public int getItemCount() {
        return orderLineItemList != null ? orderLineItemList.size() : 0;
    }

    /**
     * Checks if order contains a specific SKU.
     *
     * @param skuCode SKU code to check
     * @return true if order contains the SKU
     */
    public boolean containsSku(String skuCode) {
        return orderLineItemList.stream()
                .anyMatch(item -> item.getSkuCode().equals(skuCode));
    }

    // ==================== Domain Events (for future event sourcing) ====================

    /**
     * Order lifecycle states.
     */
    public enum OrderStatus {
        PENDING,     // Order created, awaiting confirmation
        CONFIRMED,   // Order confirmed, ready for fulfillment
        SHIPPED,     // Order shipped to customer
        DELIVERED,   // Order delivered successfully
        CANCELLED    // Order cancelled
    }
}
