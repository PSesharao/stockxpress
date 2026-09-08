package com.seshrao.stockxpress.notificationservice.strategy;

import com.seshrao.stockxpress.common.enums.NotificationType;
import com.seshrao.stockxpress.notificationservice.event.OrderPlacedEvent;

/**
 * Strategy interface for different notification types.
 * Implementations define how to send notifications through various channels.
 */
public interface NotificationStrategy {

    /**
     * Send notification for the given order event.
     *
     * @param event the order placed event
     * @throws com.seshrao.stockxpress.common.exception.ServiceUnavailableException if notification service is unavailable
     * @throws com.seshrao.stockxpress.common.exception.ValidationException if event data is invalid
     */
    void sendNotification(OrderPlacedEvent event);

    /**
     * Get the notification type supported by this strategy.
     *
     * @return the notification type
     */
    NotificationType getNotificationType();

    /**
     * Check if this notification type is enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Validate the event data before sending notification.
     *
     * @param event the order placed event
     * @return true if valid, false otherwise
     */
    default boolean validateEvent(OrderPlacedEvent event) {
        return event != null 
            && event.getEventId() != null 
            && event.getOrderNumber() != null
            && event.getOrderLineItems() != null
            && !event.getOrderLineItems().isEmpty();
    }
}