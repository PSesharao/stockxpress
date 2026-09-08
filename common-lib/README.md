# Common Library Module

A comprehensive shared library module for the StockXpress microservices application, providing common utilities, enums, exceptions, DTOs, and aspect-oriented programming (AOP) features.

## Table of Contents

- [Overview](#overview)
- [Components](#components)
  - [Enums](#enums)
  - [Utility Classes](#utility-classes)
  - [Aspects](#aspects)
  - [Annotations](#annotations)
  - [Exceptions](#exceptions)
  - [DTOs](#dtos)
- [Usage Examples](#usage-examples)
- [Dependencies](#dependencies)
- [Best Practices](#best-practices)

## Overview

The `common-lib` module serves as a centralized library for shared functionality across all StockXpress microservices. It promotes code reuse, consistency, and maintainability by providing:

- **Standardized enums** for common types (payment status, notification types, event types, etc.)
- **Utility classes** for common operations (date/time, string manipulation, validation, JSON processing)
- **AOP aspects** for cross-cutting concerns (logging, performance monitoring)
- **Custom annotations** for declarative programming
- **Exception handling** with global exception handlers
- **Common DTOs** for standardized responses

## Components

### Enums

#### 1. PaymentStatus

Represents the lifecycle states of a payment.

**Values:**
- `PENDING` - Payment is pending processing
- `AUTHORIZED` - Payment has been authorized but not captured
- `COMPLETED` - Payment has been successfully completed
- `FAILED` - Payment has failed
- `REFUNDED` - Payment has been refunded
- `CANCELLED` - Payment has been cancelled

**Methods:**
- `getDisplayName()` - Returns the human-readable display name
- `getDescription()` - Returns the description of the status
- `isFinal()` - Checks if the payment is in a final state

**Example:**
```java
PaymentStatus status = PaymentStatus.COMPLETED;
if (status.isFinal()) {
    System.out.println("Payment is finalized: " + status.getDisplayName());
}
```

#### 2. NotificationType

Defines the available notification channels.

**Values:**
- `EMAIL` - Email notification
- `SMS` - SMS notification
- `PUSH_NOTIFICATION` - Mobile push notification
- `IN_APP` - In-app notification

**Methods:**
- `getDisplayName()` - Returns the display name
- `getDescription()` - Returns the description
- `requiresExternalService()` - Checks if external service is required
- `isRealTime()` - Checks if notification is real-time
- `fromDisplayName(String)` - Gets enum from display name

**Example:**
```java
NotificationType type = NotificationType.EMAIL;
if (type.requiresExternalService()) {
    // Initialize external email service
}
```

#### 3. EventType

Defines event types for event-driven architecture.

**Values:**
- `ORDER_PLACED` - Order placed event
- `ORDER_CONFIRMED` - Order confirmed event
- `ORDER_CANCELLED` - Order cancelled event
- `INVENTORY_RESERVED` - Inventory reserved event
- `INVENTORY_RELEASED` - Inventory released event
- `NOTIFICATION_SENT` - Notification sent event

**Methods:**
- `getDisplayName()` - Returns the display name
- `getDescription()` - Returns the description
- `getCategory()` - Returns the event category
- `isOrderEvent()` - Checks if it's an order event
- `isInventoryEvent()` - Checks if it's an inventory event
- `isNotificationEvent()` - Checks if it's a notification event
- `fromDisplayName(String)` - Gets enum from display name
- `getByCategory(String)` - Gets all events of a category

**Example:**
```java
EventType event = EventType.ORDER_PLACED;
if (event.isOrderEvent()) {
    // Handle order event
    publishEvent(event);
}
```

#### 4. OrderStatus

Represents order lifecycle states (existing).

#### 5. ErrorCode

Defines standardized error codes (existing).

### Utility Classes

#### 1. DateUtil

Provides comprehensive date and time operations.

**Key Methods:**
- `now()` - Gets current LocalDateTime
- `today()` - Gets current LocalDate
- `format(LocalDateTime)` - Formats date/time to string
- `format(LocalDate)` - Formats date to string
- `format(LocalDateTime, String)` - Formats with custom pattern
- `parseDateTime(String)` - Parses string to LocalDateTime
- `parseDate(String)` - Parses string to LocalDate
- `toDate(LocalDateTime)` - Converts to java.util.Date
- `toLocalDateTime(Date)` - Converts from java.util.Date
- `daysBetween(LocalDate, LocalDate)` - Calculates days between dates
- `hoursBetween(LocalDateTime, LocalDateTime)` - Calculates hours between times
- `isPast(LocalDate)` - Checks if date is in the past
- `isFuture(LocalDate)` - Checks if date is in the future
- `addDays(LocalDate, long)` - Adds days to a date
- `addHours(LocalDateTime, long)` - Adds hours to a date/time

**Example:**
```java
LocalDateTime now = DateUtil.now();
String formatted = DateUtil.format(now);
LocalDate deliveryDate = DateUtil.addDays(DateUtil.today(), 3);
long days = DateUtil.daysBetween(LocalDate.now(), deliveryDate);
```

#### 2. StringUtil

Provides string manipulation and validation utilities.

**Key Methods:**
- `isEmpty(String)` - Checks if null or empty
- `isNotEmpty(String)` - Checks if not null and not empty
- `isBlank(String)` - Checks if null, empty, or whitespace
- `isNotBlank(String)` - Checks if not blank
- `defaultIfEmpty(String, String)` - Returns default if empty
- `defaultIfBlank(String, String)` - Returns default if blank
- `trimToNull(String)` - Trims and returns null if empty
- `trimToEmpty(String)` - Trims and returns empty if null
- `capitalize(String)` - Capitalizes first letter
- `toCamelCase(String)` - Converts to camelCase
- `join(Collection<String>, String)` - Joins strings with delimiter
- `isNumeric(String)` - Checks if contains only digits
- `isValidEmail(String)` - Validates email format
- `generateUUID()` - Generates random UUID
- `truncate(String, int)` - Truncates to max length
- `truncateWithEllipsis(String, int)` - Truncates with "..."
- `maskExceptLast(String, int, char)` - Masks string except last n chars
- `nullToEmpty(String)` - Converts null to empty
- `emptyToNull(String)` - Converts empty to null

**Example:**
```java
String email = "user@example.com";
if (StringUtil.isValidEmail(email)) {
    // Process email
}

String orderId = StringUtil.generateUUID();
String masked = StringUtil.maskExceptLast("1234567890", 4, '*'); // ******7890
```

#### 3. ValidationUtil

Provides validation helpers with exception throwing.

**Key Methods:**
- `requireNonNull(Object, String)` - Validates not null
- `requireNonEmpty(String, String)` - Validates string not empty
- `requireNonBlank(String, String)` - Validates string not blank
- `requireNonEmpty(Collection, String)` - Validates collection not empty
- `requirePositive(Number, String)` - Validates positive number
- `requireNonNegative(Number, String)` - Validates non-negative number
- `requireInRange(int, int, int, String)` - Validates value in range
- `requireInRange(BigDecimal, BigDecimal, BigDecimal, String)` - Range validation for decimals
- `requireValidEmail(String, String)` - Validates email format
- `isValidPhone(String)` - Checks phone number format
- `requirePattern(String, Pattern, String)` - Validates against pattern
- `requireLength(String, int, int, String)` - Validates string length
- `requireSize(Collection, int, int, String)` - Validates collection size
- `isNullOrEmpty(Object)` - Checks if null or empty
- `requireTrue(boolean, String)` - Validates condition is true
- `requireFalse(boolean, String)` - Validates condition is false

**Example:**
```java
public void processOrder(Order order) {
    ValidationUtil.requireNonNull(order, "Order");
    ValidationUtil.requirePositive(order.getTotalAmount(), "Total amount");
    ValidationUtil.requireNonEmpty(order.getItems(), "Order items");
    
    if (order.getEmail() != null) {
        ValidationUtil.requireValidEmail(order.getEmail(), "Email");
    }
}
```

#### 4. JsonUtil

Provides JSON serialization and deserialization utilities using Jackson.

**Key Methods:**
- `getObjectMapper()` - Gets configured ObjectMapper instance
- `toJson(Object)` - Converts object to JSON string
- `toPrettyJson(Object)` - Converts to pretty-printed JSON
- `fromJson(String, Class<T>)` - Converts JSON string to object
- `fromJson(byte[], Class<T>)` - Converts JSON bytes to object
- `toJsonBytes(Object)` - Converts object to JSON bytes
- `clone(T, Class<T>)` - Deep clones object via serialization
- `isValidJson(String)` - Validates JSON format

**Example:**
```java
Order order = new Order();
order.setOrderNumber("ORD-001");

// Serialize to JSON
String json = JsonUtil.toJson(order);
String prettyJson = JsonUtil.toPrettyJson(order);

// Deserialize from JSON
Order deserializedOrder = JsonUtil.fromJson(json, Order.class);

// Clone object
Order clonedOrder = JsonUtil.clone(order, Order.class);

// Validate JSON
if (JsonUtil.isValidJson(json)) {
    // Process valid JSON
}
```

### Aspects

#### 1. LoggingAspect

Automatic logging for service and controller methods using @Around advice.

**Features:**
- Logs method entry with parameters
- Logs method exit with return value
- Logs execution time
- Logs exceptions with stack traces
- Provides contextual information (class, method name)

**Pointcuts:**
- Service layer: `execution(* com.seshrao.stockxpress..service..*(..))`
- Controller layer: `execution(* com.seshrao.stockxpress..controller..*(..))`

**Example Output:**
```
INFO  - Entering method: OrderService.createOrder with arguments: [OrderRequest@abc123]
INFO  - Exiting method: OrderService.createOrder with result: Order@def456 | Execution time: 145 ms
```

**No configuration needed** - automatically applies to all service and controller methods.

#### 2. PerformanceLoggingAspect

Performance tracking for methods annotated with `@LogExecutionTime`.

**Features:**
- Measures and logs execution time
- Supports custom operation descriptions
- Optional parameter and return value logging
- Warning threshold for slow operations
- Separate performance logger
- Tracks success/failure status

**Configuration via annotation:**
```java
@LogExecutionTime(
    value = "Process order payment",
    logParameters = true,
    logReturnValue = true,
    warnThresholdMillis = 1000
)
public PaymentResult processPayment(Payment payment) {
    // Implementation
}
```

### Annotations

#### @LogExecutionTime

Custom annotation for declarative performance logging.

**Attributes:**
- `value` - Optional description (default: method name)
- `logParameters` - Whether to log parameters (default: false)
- `logReturnValue` - Whether to log return value (default: false)
- `warnThresholdMillis` - Warning threshold in ms (default: 0)

**Usage:**

```java
// Simple usage
@LogExecutionTime
public void processOrder(Order order) {
    // Implementation
}

// With full configuration
@LogExecutionTime(
    value = "Calculate shipping cost",
    logParameters = true,
    logReturnValue = true,
    warnThresholdMillis = 500
)
public BigDecimal calculateShipping(Address address, double weight) {
    // Implementation
}

// Class-level annotation (applies to all public methods)
@LogExecutionTime
public class OrderService {
    // All public methods will be logged
}
```

### Exceptions

Standardized exception hierarchy for consistent error handling:

- `BusinessException` - Base exception for business logic errors
- `ValidationException` - Input validation errors
- `ResourceNotFoundException` - Resource not found errors
- `ServiceUnavailableException` - Service availability errors
- `InsufficientInventoryException` - Inventory-specific errors

### DTOs

- `ErrorResponse` - Standardized error response structure
- `ValidationError` - Validation error details

## Usage Examples

### Example 1: Order Processing with Validation and Logging

```java
@Service
public class OrderService {
    
    @LogExecutionTime(value = "Create new order", warnThresholdMillis = 2000)
    public Order createOrder(OrderRequest request) {
        // Validation
        ValidationUtil.requireNonNull(request, "OrderRequest");
        ValidationUtil.requireNonEmpty(request.getItems(), "Order items");
        ValidationUtil.requirePositive(request.getTotalAmount(), "Total amount");
        
        // Create order
        Order order = new Order();
        order.setOrderNumber(StringUtil.generateUUID());
        order.setCreatedDate(DateUtil.now());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        
        // Process items
        request.getItems().forEach(item -> {
            ValidationUtil.requirePositive(item.getQuantity(), "Quantity");
        });
        
        return order;
    }
    
    @LogExecutionTime(value = "Process payment", logParameters = true)
    public void processPayment(String orderId, PaymentStatus status) {
        ValidationUtil.requireNonBlank(orderId, "Order ID");
        ValidationUtil.requireNonNull(status, "Payment status");
        
        // Update payment status
        if (status.isFinal()) {
            // Handle final payment states
            publishEvent(EventType.ORDER_CONFIRMED);
        }
    }
}
```

### Example 2: Event Publishing

```java
@Service
public class EventPublisher {
    
    public void publishOrderEvent(Order order, EventType eventType) {
        ValidationUtil.requireNonNull(order, "Order");
        ValidationUtil.requireNonNull(eventType, "Event type");
        ValidationUtil.requireTrue(eventType.isOrderEvent(), 
            "Event type must be an order event");
        
        String eventJson = JsonUtil.toJson(order);
        // Publish to message broker
    }
}
```

### Example 3: Notification Service

```java
@Service
public class NotificationService {
    
    @LogExecutionTime(value = "Send notification", warnThresholdMillis = 3000)
    public void sendNotification(String recipient, String message, 
                                NotificationType type) {
        ValidationUtil.requireNonBlank(recipient, "Recipient");
        ValidationUtil.requireNonBlank(message, "Message");
        ValidationUtil.requireNonNull(type, "Notification type");
        
        if (type == NotificationType.EMAIL) {
            ValidationUtil.requireValidEmail(recipient, "Email");
        }
        
        if (type.requiresExternalService()) {
            // Use external service
        } else {
            // Use internal notification
        }
        
        // Publish notification event
        publishEvent(EventType.NOTIFICATION_SENT);
    }
}
```

### Example 4: Date/Time Operations

```java
@Service
public class DeliveryService {
    
    public LocalDate calculateDeliveryDate(Order order) {
        LocalDate orderDate = order.getOrderDate();
        int deliveryDays = calculateDeliveryDays(order.getAddress());
        
        LocalDate estimatedDelivery = DateUtil.addDays(orderDate, deliveryDays);
        
        // Check if delivery is in future
        if (!DateUtil.isFuture(estimatedDelivery)) {
            throw new BusinessException("Invalid delivery date");
        }
        
        long daysUntilDelivery = DateUtil.daysBetween(
            DateUtil.today(), estimatedDelivery
        );
        
        return estimatedDelivery;
    }
}
```

## Dependencies

The common-lib module includes the following dependencies:

- **Spring Boot Starter Web** - For REST support
- **Spring Boot Starter AOP** - For aspect-oriented programming
- **Spring Boot Starter Validation** - For validation support
- **Lombok** - For reducing boilerplate code
- **Jackson Databind** - For JSON operations
- **Jackson Datatype JSR310** - For Java 8 date/time support
- **SLF4J** - For logging abstraction
- **Logstash Logback Encoder** - For structured logging

## Best Practices

### 1. Validation

- Always validate method parameters using `ValidationUtil`
- Throw exceptions early with descriptive field names
- Use appropriate validation methods for the data type

```java
// Good
ValidationUtil.requirePositive(price, "Product price");

// Bad
if (price <= 0) {
    throw new IllegalArgumentException("Invalid price");
}
```

### 2. Date/Time Handling

- Use `LocalDateTime` and `LocalDate` instead of `Date`
- Always use `DateUtil` for conversions and formatting
- Handle null values appropriately

```java
// Good
String formatted = DateUtil.format(dateTime);

// Bad
String formatted = dateTime.toString();
```

### 3. String Operations

- Use `StringUtil` methods instead of manual null checks
- Prefer `isBlank()` over `isEmpty()` for user input
- Use `defaultIfBlank()` for default values

```java
// Good
String name = StringUtil.defaultIfBlank(input, "Unknown");

// Bad
String name = (input == null || input.trim().isEmpty()) ? "Unknown" : input;
```

### 4. JSON Processing

- Use `JsonUtil` for all JSON operations
- Always check if JSON is valid before parsing
- Handle null values in serialization

```java
// Good
if (JsonUtil.isValidJson(jsonString)) {
    Order order = JsonUtil.fromJson(jsonString, Order.class);
}

// Bad
Order order = new ObjectMapper().readValue(jsonString, Order.class);
```

### 5. Performance Logging

- Use `@LogExecutionTime` for critical business methods
- Set appropriate warning thresholds
- Avoid logging sensitive data in parameters/return values
- Use class-level annotation sparingly

```java
// Good - Method level with threshold
@LogExecutionTime(value = "Process payment", warnThresholdMillis = 1000)
public PaymentResult processPayment(Payment payment) { ... }

// Use with caution - logs all public methods
@LogExecutionTime
public class CriticalService { ... }
```

### 6. Enums

- Use enum methods instead of switch statements
- Leverage category and type methods
- Use `fromDisplayName()` for string conversions

```java
// Good
if (eventType.isOrderEvent()) {
    handleOrderEvent(eventType);
}

// Less preferred
switch(eventType) {
    case ORDER_PLACED:
    case ORDER_CONFIRMED:
    case ORDER_CANCELLED:
        handleOrderEvent(eventType);
        break;
}
```

### 7. Exception Handling

- Use specific exception types
- Provide descriptive error messages
- Let `GlobalExceptionHandler` handle the response formatting

```java
// Good
throw new ResourceNotFoundException("Order not found with ID: " + orderId);

// Bad
throw new Exception("Error");
```

## Package Structure

```
com.seshrao.stockxpress.common
├── annotation
│   └── LogExecutionTime.java
├── aspect
│   ├── LoggingAspect.java
│   └── PerformanceLoggingAspect.java
├── constants
│   └── ApplicationConstants.java
├── dto
│   ├── ErrorResponse.java
│   └── ValidationError.java
├── enums
│   ├── ErrorCode.java
│   ├── EventType.java
│   ├── NotificationType.java
│   ├── OrderStatus.java
│   └── PaymentStatus.java
├── exception
│   ├── BusinessException.java
│   ├── InsufficientInventoryException.java
│   ├── ResourceNotFoundException.java
│   ├── ServiceUnavailableException.java
│   └── ValidationException.java
├── handler
│   └── GlobalExceptionHandler.java
└── util
    ├── DateUtil.java
    ├── JsonUtil.java
    ├── StringUtil.java
    └── ValidationUtil.java
```

## Contributing

When adding new components to the common-lib:

1. Follow existing patterns and conventions
2. Add comprehensive JavaDoc documentation
3. Include usage examples in JavaDoc
4. Write unit tests for all new utilities
5. Update this README with new components
6. Ensure thread-safety for utility classes
7. Make utility classes final with private constructors
8. Handle null values gracefully

## Version

**Current Version:** 1.0-SNAPSHOT

## License

Copyright © 2024 StockXpress. All rights reserved.
