# Common-Lib Configuration Guide

## Quick Start

This guide will help you integrate the common-lib module into your microservices.

## 1. Add Dependency

Add the following dependency to your microservice's `pom.xml`:

```xml
<dependency>
    <groupId>com.seshrao.stockxpress</groupId>
    <artifactId>common-lib</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 2. Component Scan Configuration

Ensure your Spring Boot application scans the common-lib packages. Add this to your main application class:

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.seshrao.stockxpress.yourservice",
    "com.seshrao.stockxpress.common"  // Scan common-lib components
})
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

## 3. Enable AOP (AspectJ)

Add `@EnableAspectJAutoProxy` to your configuration class or main application:

```java
@SpringBootApplication
@EnableAspectJAutoProxy
public class YourServiceApplication {
    // ...
}
```

## 4. Logging Configuration

### application.properties

```properties
# Logging levels
logging.level.com.seshrao.stockxpress=INFO
logging.level.com.seshrao.stockxpress.common.aspect=DEBUG
logging.level.performance=INFO

# Log pattern
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Log file
logging.file.name=logs/application.log
logging.file.max-size=10MB
logging.file.max-history=30
```

### logback-spring.xml (Optional - for advanced configuration)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>
    
    <!-- Performance logger -->
    <appender name="PERFORMANCE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/performance.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/performance.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="performance" level="INFO" additivity="false">
        <appender-ref ref="PERFORMANCE"/>
    </logger>
    
    <!-- Aspect loggers -->
    <logger name="com.seshrao.stockxpress.common.aspect" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

## 5. Using Components in Your Service

### Service Layer Example

```java
package com.seshrao.stockxpress.yourservice.service;

import com.seshrao.stockxpress.common.annotation.LogExecutionTime;
import com.seshrao.stockxpress.common.enums.*;
import com.seshrao.stockxpress.common.exception.BusinessException;
import com.seshrao.stockxpress.common.util.*;
import org.springframework.stereotype.Service;

@Service
public class YourService {
    
    @LogExecutionTime(value = "Process business logic", warnThresholdMillis = 1000)
    public Result processData(Request request) {
        // Validation
        ValidationUtil.requireNonNull(request, "Request");
        ValidationUtil.requireNonBlank(request.getId(), "Request ID");
        
        // Business logic
        String uniqueId = StringUtil.generateUUID();
        String timestamp = DateUtil.format(DateUtil.now());
        
        // Create result
        Result result = new Result();
        result.setId(uniqueId);
        result.setTimestamp(timestamp);
        
        return result;
    }
}
```

### Controller Layer Example

```java
package com.seshrao.stockxpress.yourservice.controller;

import com.seshrao.stockxpress.common.dto.ErrorResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class YourController {
    
    private final YourService yourService;
    
    // LoggingAspect will automatically log this method
    @PostMapping("/process")
    public ResponseEntity<Result> process(@RequestBody Request request) {
        Result result = yourService.processData(request);
        return ResponseEntity.ok(result);
    }
}
```

## 6. Exception Handling

The `GlobalExceptionHandler` is automatically active. Just throw exceptions:

```java
public class YourService {
    
    public void doSomething(String id) {
        if (StringUtil.isBlank(id)) {
            throw new ValidationException("ID is required");
        }
        
        YourEntity entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Entity not found with ID: " + id
            ));
        
        if (!isValid(entity)) {
            throw new BusinessException("Entity is not valid");
        }
    }
}
```

## 7. Event Publishing Pattern

```java
@Service
public class EventPublisherService {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    public void publishOrderEvent(Order order, EventType eventType) {
        ValidationUtil.requireTrue(
            eventType.isOrderEvent(), 
            "Event must be an order event"
        );
        
        String eventJson = JsonUtil.toJson(order);
        kafkaTemplate.send("order-events", eventJson);
    }
}
```

## 8. Notification Service Pattern

```java
@Service
public class NotificationService {
    
    @LogExecutionTime(value = "Send notification", warnThresholdMillis = 2000)
    public void sendNotification(String recipient, String message, 
                                NotificationType type) {
        ValidationUtil.requireNonBlank(recipient, "Recipient");
        
        if (type == NotificationType.EMAIL) {
            ValidationUtil.requireValidEmail(recipient, "Email");
            sendEmail(recipient, message);
        } else if (type == NotificationType.SMS) {
            sendSMS(recipient, message);
        } else if (type == NotificationType.PUSH_NOTIFICATION) {
            sendPushNotification(recipient, message);
        }
    }
}
```

## 9. Testing Components

### Unit Test Example

```java
import com.seshrao.stockxpress.common.util.StringUtil;
import com.seshrao.stockxpress.common.util.ValidationUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class YourServiceTest {
    
    @Test
    void testValidation() {
        String email = "test@example.com";
        assertTrue(StringUtil.isValidEmail(email));
        
        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtil.requireNonBlank("", "Field");
        });
    }
}
```

## 10. Performance Monitoring

### Enable Performance Logging

Simply annotate critical methods with `@LogExecutionTime`:

```java
@Service
public class CriticalService {
    
    // Logs execution time, warns if > 500ms
    @LogExecutionTime(
        value = "Critical operation",
        warnThresholdMillis = 500,
        logParameters = true,
        logReturnValue = false
    )
    public Result criticalOperation(Request request) {
        // Implementation
    }
}
```

### Monitor Performance Logs

Check `logs/performance.log` for execution time metrics:

```
2024-01-15 10:30:45 - [PERFORMANCE] Completed: Critical operation | Time: 234 ms | Method: CriticalService.criticalOperation | Status: SUCCESS
2024-01-15 10:31:12 - [PERFORMANCE] Completed: Process payment | Time: 1234 ms | Method: PaymentService.processPayment | Status: SUCCESS | Exceeded threshold of 1000 ms
```

## 11. Common Patterns

### Pattern 1: Request Validation

```java
public void processRequest(OrderRequest request) {
    // All-in-one validation
    ValidationUtil.requireNonNull(request, "OrderRequest");
    ValidationUtil.requireNonBlank(request.getCustomerId(), "Customer ID");
    ValidationUtil.requirePositive(request.getAmount(), "Amount");
    ValidationUtil.requireNonEmpty(request.getItems(), "Order items");
}
```

### Pattern 2: Safe String Handling

```java
public String processUserInput(String input) {
    // Always safe from null/blank
    String cleaned = StringUtil.trimToNull(input);
    return StringUtil.defaultIfBlank(cleaned, "DEFAULT_VALUE");
}
```

### Pattern 3: Date Range Calculations

```java
public List<Order> getOrdersInRange(LocalDate startDate, LocalDate endDate) {
    ValidationUtil.requireTrue(
        DateUtil.daysBetween(startDate, endDate) > 0,
        "End date must be after start date"
    );
    
    return orderRepository.findByDateRange(startDate, endDate);
}
```

### Pattern 4: JSON Safe Operations

```java
public void processJsonData(String jsonString) {
    if (!JsonUtil.isValidJson(jsonString)) {
        throw new ValidationException("Invalid JSON format");
    }
    
    Order order = JsonUtil.fromJson(jsonString, Order.class);
    if (order == null) {
        throw new BusinessException("Failed to parse order");
    }
    
    // Process order
}
```

## 12. Troubleshooting

### Aspects Not Working

**Problem:** LoggingAspect or PerformanceLoggingAspect not intercepting methods.

**Solutions:**
1. Ensure `@EnableAspectJAutoProxy` is present
2. Check component scanning includes `com.seshrao.stockxpress.common`
3. Verify AspectJ weaver is on classpath
4. Check method is public (aspects only work on public methods)
5. Ensure method is called from outside the class (not `this.method()`)

### Global Exception Handler Not Working

**Problem:** Exceptions not being caught by GlobalExceptionHandler.

**Solutions:**
1. Ensure common-lib is in component scan
2. Check no other `@ControllerAdvice` is overriding it
3. Verify exception is being thrown from a `@Controller` or `@RestController`

### Performance Logs Not Appearing

**Problem:** Performance logs not showing in logs/performance.log.

**Solutions:**
1. Check logging configuration includes `performance` logger
2. Verify `@LogExecutionTime` annotation is on method
3. Ensure method is being called (aspect is active)
4. Check log level is set to INFO or lower for performance logger

## 13. Best Practices Summary

1. **Always validate inputs** using `ValidationUtil`
2. **Use StringUtil** instead of manual null checks
3. **Prefer LocalDateTime/LocalDate** over Date
4. **Use JsonUtil** for all JSON operations
5. **Annotate performance-critical methods** with `@LogExecutionTime`
6. **Throw specific exceptions** (ValidationException, BusinessException, etc.)
7. **Use enum helper methods** instead of switch statements
8. **Set appropriate warn thresholds** for performance logging
9. **Don't log sensitive data** (passwords, tokens, etc.)
10. **Handle null values gracefully** using utility methods

## 14. Migration Guide

### Migrating from Manual Validation

**Before:**
```java
if (email == null || email.trim().isEmpty()) {
    throw new IllegalArgumentException("Email is required");
}
if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
    throw new IllegalArgumentException("Invalid email");
}
```

**After:**
```java
ValidationUtil.requireNonBlank(email, "Email");
ValidationUtil.requireValidEmail(email, "Email");
```

### Migrating from Manual JSON Handling

**Before:**
```java
ObjectMapper mapper = new ObjectMapper();
try {
    String json = mapper.writeValueAsString(order);
    return json;
} catch (JsonProcessingException e) {
    logger.error("JSON error", e);
    return null;
}
```

**After:**
```java
return JsonUtil.toJson(order);
```

### Migrating to Aspect Logging

**Before:**
```java
public Order processOrder(Order order) {
    logger.info("Processing order: {}", order.getId());
    long start = System.currentTimeMillis();
    try {
        // process
        logger.info("Order processed in {} ms", System.currentTimeMillis() - start);
        return result;
    } catch (Exception e) {
        logger.error("Error processing order", e);
        throw e;
    }
}
```

**After:**
```java
@LogExecutionTime(value = "Process order", warnThresholdMillis = 1000)
public Order processOrder(Order order) {
    // Just implement business logic
    return result;
}
```

## 15. Additional Resources

- **README.md** - Complete component documentation
- **CommonLibUsageExample.java** - Comprehensive usage examples
- **Source code JavaDoc** - Detailed API documentation

## Support

For issues or questions:
1. Check this guide and README.md
2. Review CommonLibUsageExample.java
3. Check JavaDoc in source code
4. Contact the StockXpress development team
