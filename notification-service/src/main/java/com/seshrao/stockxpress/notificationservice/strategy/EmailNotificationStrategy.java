package com.seshrao.stockxpress.notificationservice.strategy;

import com.seshrao.stockxpress.common.annotation.LogExecutionTime;
import com.seshrao.stockxpress.common.enums.NotificationType;
import com.seshrao.stockxpress.common.exception.ServiceUnavailableException;
import com.seshrao.stockxpress.common.exception.ValidationException;
import com.seshrao.stockxpress.notificationservice.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Email notification strategy implementation.
 * Sends email notifications for order events.
 */
@Component
@Slf4j
public class EmailNotificationStrategy implements NotificationStrategy {

    @Value("${notification.email.enabled:true}")
    private boolean enabled;

    @Value("${notification.email.from:noreply@stockxpress.com}")
    private String fromEmail;

    @Value("${notification.email.to:customer@stockxpress.com}")
    private String toEmail;

    @Override
    @LogExecutionTime(value = "Email Notification", warnThresholdMillis = 2000)
    public void sendNotification(OrderPlacedEvent event) {
        log.info("Processing email notification for order: {}", event.getOrderNumber());

        if (!isEnabled()) {
            log.warn("Email notifications are disabled");
            return;
        }

        if (!validateEvent(event)) {
            log.error("Invalid event data for email notification: eventId={}", event.getEventId());
            throw new ValidationException("Invalid order event data for email notification");
        }

        try {
            sendEmailNotification(event);
            log.info("Email notification sent successfully for order: {}, eventId: {}", 
                event.getOrderNumber(), event.getEventId());
        } catch (Exception ex) {
            log.error("Failed to send email notification for order: {}, eventId: {}, error: {}",
                event.getOrderNumber(), event.getEventId(), ex.getMessage(), ex);
            throw new ServiceUnavailableException("Email service unavailable: " + ex.getMessage());
        }
    }

    /**
     * Send email notification (simulated).
     * In production, this would integrate with email service (SendGrid, AWS SES, etc.)
     */
    private void sendEmailNotification(OrderPlacedEvent event) {
        String emailContent = buildEmailContent(event);
        
        // TODO: Integrate with actual email service
        log.info("=== EMAIL NOTIFICATION ===");
        log.info("From: {}", fromEmail);
        log.info("To: {}", toEmail);
        log.info("Subject: Order Confirmation - {}", event.getOrderNumber());
        log.info("Content:\n{}", emailContent);
        log.info("===========================");

        // Simulate email sending delay
        simulateEmailSending();
    }

    /**
     * Build email content from order event.
     */
    private String buildEmailContent(OrderPlacedEvent event) {
        StringBuilder content = new StringBuilder();
        content.append("Dear Customer,\n\n");
        content.append("Thank you for your order!\n\n");
        content.append("Order Details:\n");
        content.append("Order Number: ").append(event.getOrderNumber()).append("\n");
        content.append("Order ID: ").append(event.getOrderId()).append("\n");
        content.append("Order Date: ").append(event.getTimestamp()).append("\n\n");
        
        content.append("Items Ordered:\n");
        content.append("-------------------\n");
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderPlacedEvent.OrderLineItemEvent item : event.getOrderLineItems()) {
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
            
            content.append(String.format("- %s (SKU: %s)\n", item.getSkuCode(), item.getSkuCode()));
            content.append(String.format("  Quantity: %d\n", item.getQuantity()));
            content.append(String.format("  Price: $%.2f\n", item.getPrice()));
            content.append(String.format("  Subtotal: $%.2f\n\n", itemTotal));
        }
        
        content.append("-------------------\n");
        content.append(String.format("Total Amount: $%.2f\n\n", totalAmount));
        content.append("We will notify you once your order is shipped.\n\n");
        content.append("Best regards,\n");
        content.append("StockXpress Team");
        
        return content.toString();
    }

    /**
     * Simulate email sending delay.
     */
    private void simulateEmailSending() {
        try {
            Thread.sleep(100); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Email sending simulation interrupted");
        }
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}