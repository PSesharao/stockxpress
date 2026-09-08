package com.seshrao.stockxpress.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Improved Order Placed Event with comprehensive metadata.
 * Contains full order information for event-driven architecture.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderPlacedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    // Event metadata
    private String eventId;              // Unique identifier for this event
    private String eventVersion;         // Event schema version for compatibility
    private LocalDateTime timestamp;     // When the event was created

    // Order information
    private String orderNumber;          // Business order number
    private Long orderId;                // Database order ID

    // Order line items
    private List<OrderLineItemEvent> orderLineItems;

    // Additional metadata (source, environment, etc.)
    private Map<String, Object> metadata;

    /**
     * Nested class representing order line item in the event.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OrderLineItemEvent implements Serializable {
        private static final long serialVersionUID = 1L;

        private String skuCode;
        private BigDecimal price;
        private Integer quantity;
    }
}
