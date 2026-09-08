package com.seshrao.stockxpress.common.examples;

import com.seshrao.stockxpress.common.annotation.LogExecutionTime;
import com.seshrao.stockxpress.common.enums.EventType;
import com.seshrao.stockxpress.common.enums.NotificationType;
import com.seshrao.stockxpress.common.enums.OrderStatus;
import com.seshrao.stockxpress.common.enums.PaymentStatus;
import com.seshrao.stockxpress.common.exception.BusinessException;
import com.seshrao.stockxpress.common.exception.ValidationException;
import com.seshrao.stockxpress.common.util.DateUtil;
import com.seshrao.stockxpress.common.util.JsonUtil;
import com.seshrao.stockxpress.common.util.StringUtil;
import com.seshrao.stockxpress.common.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Example service demonstrating the usage of all common-lib components.
 * This is a reference implementation showing best practices.
 * 
 * @author StockXpress Team
 * @version 1.0
 */
@Service
public class CommonLibUsageExample {

    private static final Logger logger = LoggerFactory.getLogger(CommonLibUsageExample.class);

    /**
     * Example 1: Using ValidationUtil for input validation.
     * Demonstrates comprehensive validation of method parameters.
     */
    @LogExecutionTime(value = "Validate order request", logParameters = true)
    public void validateOrderRequest(String customerEmail, BigDecimal totalAmount, 
                                     List<String> items, String orderDate) {
        // Validate non-null values
        ValidationUtil.requireNonNull(items, "Order items");
        ValidationUtil.requireNonNull(totalAmount, "Total amount");
        
        // Validate string values
        ValidationUtil.requireNonBlank(customerEmail, "Customer email");
        ValidationUtil.requireValidEmail(customerEmail, "Customer email");
        
        // Validate numeric values
        ValidationUtil.requirePositive(totalAmount, "Total amount");
        ValidationUtil.requireInRange(totalAmount, 
            new BigDecimal("0.01"), new BigDecimal("10000.00"), "Total amount");
        
        // Validate collection
        ValidationUtil.requireNonEmpty(items, "Order items");
        ValidationUtil.requireSize(items, 1, 100, "Order items");
        
        logger.info("Order validation successful for email: {}", customerEmail);
    }

    /**
     * Example 2: Using StringUtil for string operations.
     * Demonstrates various string manipulation and validation operations.
     */
    public String processCustomerData(String name, String email, String phone) {
        // Handle null/empty strings
        String processedName = StringUtil.defaultIfBlank(name, "Guest");
        String trimmedEmail = StringUtil.trimToNull(email);
        
        // Validate email
        if (StringUtil.isNotBlank(trimmedEmail) && !StringUtil.isValidEmail(trimmedEmail)) {
            throw new ValidationException("Invalid email format");
        }
        
        // Generate unique ID
        String customerId = StringUtil.generateUUID();
        
        // Mask sensitive data
        String maskedPhone = StringUtil.maskExceptLast(phone, 4, '*');
        
        // Capitalize name
        String capitalizedName = StringUtil.capitalize(processedName);
        
        logger.info("Processed customer: {} with ID: {}", capitalizedName, customerId);
        return customerId;
    }

    /**
     * Example 3: Using DateUtil for date/time operations.
     * Demonstrates date formatting, parsing, and calculations.
     */
    @LogExecutionTime(value = "Calculate delivery schedule", warnThresholdMillis = 500)
    public DeliverySchedule calculateDeliverySchedule(int standardDeliveryDays) {
        // Get current date/time
        LocalDateTime orderTime = DateUtil.now();
        LocalDate orderDate = DateUtil.today();
        
        // Calculate delivery date
        LocalDate estimatedDelivery = DateUtil.addDays(orderDate, standardDeliveryDays);
        
        // Validate delivery date is in future
        if (!DateUtil.isFuture(estimatedDelivery)) {
            throw new BusinessException("Delivery date must be in the future");
        }
        
        // Calculate days until delivery
        long daysUntilDelivery = DateUtil.daysBetween(orderDate, estimatedDelivery);
        
        // Format dates for display
        String formattedOrderTime = DateUtil.format(orderTime);
        String formattedDeliveryDate = DateUtil.format(estimatedDelivery);
        
        DeliverySchedule schedule = new DeliverySchedule();
        schedule.orderTime = formattedOrderTime;
        schedule.deliveryDate = formattedDeliveryDate;
        schedule.daysUntilDelivery = daysUntilDelivery;
        
        logger.info("Delivery scheduled for: {} ({} days)", 
            formattedDeliveryDate, daysUntilDelivery);
        
        return schedule;
    }

    /**
     * Example 4: Using JsonUtil for JSON operations.
     * Demonstrates serialization, deserialization, and validation.
     */
    public String processOrderAsJson(Order order) {
        // Validate order
        ValidationUtil.requireNonNull(order, "Order");
        
        // Convert to JSON
        String orderJson = JsonUtil.toJson(order);
        
        if (orderJson == null) {
            throw new BusinessException("Failed to serialize order");
        }
        
        // Validate JSON
        if (!JsonUtil.isValidJson(orderJson)) {
            throw new BusinessException("Generated invalid JSON");
        }
        
        // Clone order (deep copy)
        Order clonedOrder = JsonUtil.clone(order, Order.class);
        
        // Pretty print for logging
        String prettyJson = JsonUtil.toPrettyJson(order);
        logger.debug("Order JSON: {}", prettyJson);
        
        return orderJson;
    }

    /**
     * Example 5: Using PaymentStatus enum.
     * Demonstrates enum usage and status checks.
     */
    @LogExecutionTime(value = "Process payment status", logReturnValue = true)
    public boolean processPaymentStatus(PaymentStatus status) {
        ValidationUtil.requireNonNull(status, "Payment status");
        
        logger.info("Processing payment status: {} - {}", 
            status.getDisplayName(), status.getDescription());
        
        // Check if payment is finalized
        if (status.isFinal()) {
            logger.info("Payment is in final state: {}", status);
            return true;
        }
        
        // Handle different statuses
        switch (status) {
            case PENDING:
                logger.info("Payment is pending processing");
                break;
            case AUTHORIZED:
                logger.info("Payment authorized, ready for capture");
                break;
            default:
                logger.warn("Unexpected payment status: {}", status);
        }
        
        return false;
    }

    /**
     * Example 6: Using NotificationType enum.
     * Demonstrates notification type selection and validation.
     */
    public void sendNotification(NotificationType type, String recipient, String message) {
        ValidationUtil.requireNonNull(type, "Notification type");
        ValidationUtil.requireNonBlank(recipient, "Recipient");
        ValidationUtil.requireNonBlank(message, "Message");
        
        logger.info("Sending {} notification to: {}", type.getDisplayName(), recipient);
        
        // Check if external service is required
        if (type.requiresExternalService()) {
            logger.info("Using external service for {}", type.getDisplayName());
            // Initialize external service
        }
        
        // Check if real-time notification
        if (type.isRealTime()) {
            logger.info("Sending real-time notification");
            // Send immediately
        } else {
            logger.info("Queuing notification for batch processing");
            // Queue for later
        }
        
        // Validate recipient based on type
        if (type == NotificationType.EMAIL) {
            ValidationUtil.requireValidEmail(recipient, "Email recipient");
        } else if (type == NotificationType.SMS) {
            if (!ValidationUtil.isValidPhone(recipient)) {
                throw new ValidationException("Invalid phone number");
            }
        }
    }

    /**
     * Example 7: Using EventType enum.
     * Demonstrates event type categorization and filtering.
     */
    @LogExecutionTime(value = "Publish event", logParameters = true)
    public void publishEvent(EventType eventType, String payload) {
        ValidationUtil.requireNonNull(eventType, "Event type");
        ValidationUtil.requireNonBlank(payload, "Event payload");
        
        logger.info("Publishing event: {} - {}", 
            eventType.getDisplayName(), eventType.getDescription());
        
        // Route based on category
        if (eventType.isOrderEvent()) {
            logger.info("Routing to order event handler");
            publishToOrderTopic(eventType, payload);
        } else if (eventType.isInventoryEvent()) {
            logger.info("Routing to inventory event handler");
            publishToInventoryTopic(eventType, payload);
        } else if (eventType.isNotificationEvent()) {
            logger.info("Routing to notification event handler");
            publishToNotificationTopic(eventType, payload);
        }
        
        // Log event category
        logger.debug("Event category: {}", eventType.getCategory());
    }

    /**
     * Example 8: Complete order processing workflow.
     * Demonstrates integration of multiple components.
     */
    @LogExecutionTime(
        value = "Complete order processing workflow",
        logParameters = true,
        warnThresholdMillis = 2000
    )
    public OrderResult processCompleteOrder(OrderRequest request) {
        try {
            // Step 1: Validate input
            validateOrderRequest(
                request.customerEmail,
                request.totalAmount,
                request.items,
                request.orderDate
            );
            
            // Step 2: Create order
            Order order = new Order();
            order.orderId = StringUtil.generateUUID();
            order.customerEmail = request.customerEmail;
            order.totalAmount = request.totalAmount;
            order.items = request.items;
            order.status = OrderStatus.PENDING;
            order.paymentStatus = PaymentStatus.PENDING;
            order.createdAt = DateUtil.format(DateUtil.now());
            
            // Step 3: Calculate delivery
            DeliverySchedule schedule = calculateDeliverySchedule(3);
            order.estimatedDelivery = schedule.deliveryDate;
            
            // Step 4: Convert to JSON for persistence
            String orderJson = processOrderAsJson(order);
            
            // Step 5: Publish order placed event
            publishEvent(EventType.ORDER_PLACED, orderJson);
            
            // Step 6: Reserve inventory
            publishEvent(EventType.INVENTORY_RESERVED, orderJson);
            
            // Step 7: Send confirmation notification
            sendNotification(
                NotificationType.EMAIL,
                order.customerEmail,
                "Order confirmed: " + order.orderId
            );
            
            // Step 8: Create result
            OrderResult result = new OrderResult();
            result.orderId = order.orderId;
            result.status = "SUCCESS";
            result.message = "Order processed successfully";
            
            logger.info("Order processing completed successfully: {}", order.orderId);
            return result;
            
        } catch (ValidationException e) {
            logger.error("Validation error during order processing: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error processing order: {}", e.getMessage(), e);
            throw new BusinessException("Failed to process order: " + e.getMessage(), e);
        }
    }

    // Helper methods for event publishing
    private void publishToOrderTopic(EventType eventType, String payload) {
        logger.debug("Publishing to order topic: {}", eventType);
    }

    private void publishToInventoryTopic(EventType eventType, String payload) {
        logger.debug("Publishing to inventory topic: {}", eventType);
    }

    private void publishToNotificationTopic(EventType eventType, String payload) {
        logger.debug("Publishing to notification topic: {}", eventType);
    }

    // Inner classes for examples
    public static class DeliverySchedule {
        String orderTime;
        String deliveryDate;
        long daysUntilDelivery;
    }

    public static class Order {
        String orderId;
        String customerEmail;
        BigDecimal totalAmount;
        List<String> items;
        OrderStatus status;
        PaymentStatus paymentStatus;
        String createdAt;
        String estimatedDelivery;
    }

    public static class OrderRequest {
        String customerEmail;
        BigDecimal totalAmount;
        List<String> items = new ArrayList<>();
        String orderDate;
    }

    public static class OrderResult {
        String orderId;
        String status;
        String message;
    }
}
