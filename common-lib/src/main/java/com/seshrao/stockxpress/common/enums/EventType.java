package com.seshrao.stockxpress.common.enums;

/**
 * Enum representing different event types in the system.
 * This enum is used for event-driven architecture and messaging.
 * 
 * @author StockXpress Team
 * @version 1.0
 */
public enum EventType {
    
    /**
     * Event triggered when an order is placed.
     */
    ORDER_PLACED("Order Placed", "A new order has been placed", "order"),
    
    /**
     * Event triggered when an order is confirmed.
     */
    ORDER_CONFIRMED("Order Confirmed", "Order has been confirmed", "order"),
    
    /**
     * Event triggered when an order is cancelled.
     */
    ORDER_CANCELLED("Order Cancelled", "Order has been cancelled", "order"),
    
    /**
     * Event triggered when inventory is reserved.
     */
    INVENTORY_RESERVED("Inventory Reserved", "Inventory has been reserved for an order", "inventory"),
    
    /**
     * Event triggered when inventory is released.
     */
    INVENTORY_RELEASED("Inventory Released", "Reserved inventory has been released", "inventory"),
    
    /**
     * Event triggered when a notification is sent.
     */
    NOTIFICATION_SENT("Notification Sent", "A notification has been sent", "notification");

    private final String displayName;
    private final String description;
    private final String category;

    /**
     * Constructor for EventType enum.
     *
     * @param displayName the display name of the event
     * @param description the description of the event
     * @param category the category of the event (order, inventory, notification, etc.)
     */
    EventType(String displayName, String description, String category) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
    }

    /**
     * Gets the display name of the event type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of the event type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the category of the event type.
     *
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Checks if this is an order-related event.
     *
     * @return true if the event is order-related, false otherwise
     */
    public boolean isOrderEvent() {
        return "order".equals(category);
    }

    /**
     * Checks if this is an inventory-related event.
     *
     * @return true if the event is inventory-related, false otherwise
     */
    public boolean isInventoryEvent() {
        return "inventory".equals(category);
    }

    /**
     * Checks if this is a notification-related event.
     *
     * @return true if the event is notification-related, false otherwise
     */
    public boolean isNotificationEvent() {
        return "notification".equals(category);
    }

    /**
     * Gets the event type from display name.
     *
     * @param displayName the display name to search for
     * @return the matching EventType, or null if not found
     */
    public static EventType fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }
        for (EventType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Gets the event type from category.
     *
     * @param category the category to search for
     * @return array of EventTypes matching the category
     */
    public static EventType[] getByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return new EventType[0];
        }
        return java.util.Arrays.stream(values())
                .filter(e -> category.equalsIgnoreCase(e.category))
                .toArray(EventType[]::new);
    }
}
