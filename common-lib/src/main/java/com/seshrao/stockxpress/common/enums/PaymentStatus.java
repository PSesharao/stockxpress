package com.seshrao.stockxpress.common.enums;

/**
 * Enum representing the status of a payment.
 */
public enum PaymentStatus {
    
    /**
     * Payment is pending processing.
     */
    PENDING("Pending", "Payment is pending"),
    
    /**
     * Payment has been authorized but not captured.
     */
    AUTHORIZED("Authorized", "Payment has been authorized"),
    
    /**
     * Payment has been successfully completed.
     */
    COMPLETED("Completed", "Payment completed successfully"),
    
    /**
     * Payment has failed.
     */
    FAILED("Failed", "Payment failed"),
    
    /**
     * Payment has been refunded.
     */
    REFUNDED("Refunded", "Payment has been refunded"),
    
    /**
     * Payment has been cancelled.
     */
    CANCELLED("Cancelled", "Payment has been cancelled");

    private final String displayName;
    private final String description;

    PaymentStatus(String displayName, String description) {
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
     * Checks if the payment is in a final state.
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED || this == CANCELLED;
    }
}