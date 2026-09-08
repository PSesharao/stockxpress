package com.seshrao.stockxpress.orderservice.publisher;

import com.seshrao.stockxpress.orderservice.model.Order;

/**
 * Publisher interface for order-related events.
 * Abstracts the event publishing mechanism from the business logic.
 */
public interface OrderEventPublisher {
    
    /**
     * Publishes an order placed event.
     *
     * @param order The order that was placed
     */
    void publishOrderPlaced(Order order);
}
