package com.seshrao.stockxpress.common.enums;

/**
 * Order status enumeration for tracking order lifecycle.
 * 
 * @author StockXpress Team
 */
public enum OrderStatus {
    
    /**
     * Order has been created but not yet confirmed
     */
    PENDING("Pending", "Order is awaiting confirmation"),
    
    /**
     * Order has been confirmed and is being processed
     */
    CONFIRMED("Confirmed", "Order has been confirmed"),
    
    /**
     * Order is currently being processed
     */
    PROCESSING("Processing", "Order is being processed"),
    
    /**
     * Order has been shipped
     */
    SHIPPED("Shipped", "Order has been shipped"),
    
    /**
     * Order has been delivered to customer
     */
    DELIVERED("Delivered", "Order has been delivered"),
    
    /**
     * Order has been cancelled by customer or system
     */
    CANCELLED("Cancelled", "Order has been cancelled"),
    
    /**
     * Order was rejected (e.g., insufficient inventory, payment failed)
     */
    REJECTED("Rejected", "Order was rejected"),
    
    /**
     * Order processing failed
     */
    FAILED("Failed", "Order processing failed");

    private final String displayName;
    private final String description;

    OrderStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if order can be cancelled
     * @return true if order can be cancelled
     */
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Check if order is in final state
     * @return true if order is in final state
     */
    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED || this == REJECTED || this == FAILED;
    }
}