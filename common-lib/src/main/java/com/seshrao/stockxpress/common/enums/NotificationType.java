package com.seshrao.stockxpress.common.enums;

/**
 * Enum representing the type of notification.
 * This enum defines various notification channels available in the system.
 * 
 * @author StockXpress Team
 * @version 1.0
 */
public enum NotificationType {
    
    /**
     * Email notification type.
     */
    EMAIL("Email", "Email notification", true),
    
    /**
     * SMS notification type.
     */
    SMS("SMS", "SMS notification", true),
    
    /**
     * Push notification type for mobile devices.
     */
    PUSH_NOTIFICATION("Push Notification", "Mobile push notification", true),
    
    /**
     * In-app notification type.
     */
    IN_APP("In-App", "In-app notification", false);

    private final String displayName;
    private final String description;
    private final boolean requiresExternalService;

    /**
     * Constructor for NotificationType enum.
     *
     * @param displayName the display name of the notification type
     * @param description the description of the notification type
     * @param requiresExternalService whether this notification requires an external service
     */
    NotificationType(String displayName, String description, boolean requiresExternalService) {
        this.displayName = displayName;
        this.description = description;
        this.requiresExternalService = requiresExternalService;
    }

    /**
     * Gets the display name of the notification type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of the notification type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this notification type requires an external service.
     *
     * @return true if external service is required, false otherwise
     */
    public boolean requiresExternalService() {
        return requiresExternalService;
    }

    /**
     * Checks if this notification type is real-time.
     *
     * @return true if notification is real-time (PUSH or IN_APP), false otherwise
     */
    public boolean isRealTime() {
        return this == PUSH_NOTIFICATION || this == IN_APP;
    }

    /**
     * Gets the notification type from display name.
     *
     * @param displayName the display name to search for
     * @return the matching NotificationType, or null if not found
     */
    public static NotificationType fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }
        for (NotificationType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        return null;
    }
}
