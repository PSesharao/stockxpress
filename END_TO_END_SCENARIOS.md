# StockXpress: End-to-End Business Scenarios

This document maps business flows to exact REST endpoints and events for the StockXpress e-commerce platform.

---

## Scenario 1: Browse Product Catalog

**Business Goal:** Customer browses available products to find items to purchase

### Flow Map

```mermaid
sequenceDiagram
    participant Customer
    participant Gateway as API Gateway<br/>:8080
    participant Product as Product Service<br/>:8081
    participant MongoDB
    participant Zipkin as Zipkin Tracing

    Customer->>Gateway: GET /api/product
    Gateway->>Gateway: Validate JWT Token
    Gateway->>Product: GET /api/product
    Product->>MongoDB: db.product.find()
    MongoDB-->>Product: Product Documents
    Product-->>Gateway: 200 OK + Product List JSON
    Gateway-->>Customer: Product List
    
    Note over Gateway,Product: All steps traced in Zipkin
    Product->>Zipkin: Send trace spans
```

### Technical Details

| Step | Component | Endpoint/Action | Request | Response |
|------|-----------|----------------|---------|----------|
| 1 | Customer | Initial request | `GET http://localhost:8080/api/product` | - |
| 2 | API Gateway | JWT validation | Header: `Authorization: Bearer <token>` | - |
| 3 | API Gateway | Route to service | Forwards to `http://product-service:8081/api/product` | - |
| 4 | Product Service | Query database | MongoDB: `db.product.find({})` | - |
| 5 | Product Service | Return results | - | HTTP 200, JSON array |
| 6 | API Gateway | Forward response | - | Product list to customer |

### Exact REST Endpoint

```http
GET /api/product HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

### Response Example

```json
[
  {
    "id": "64a1b2c3d4e5f6789abcdef0",
    "name": "iPhone 15 Pro",
    "description": "Latest Apple flagship smartphone with A17 Pro chip",
    "price": 999.99
  },
  {
    "id": "64a1b2c3d4e5f6789abcdef1",
    "name": "Samsung Galaxy S24",
    "description": "Premium Android smartphone with Galaxy AI",
    "price": 899.99
  }
]
```

### Events: None (Read-only operation)

### Cache Behavior

- **L1 Cache (Caffeine):** Product list cached for 5 minutes
- **L2 Cache (Redis):** Fallback cache for 15 minutes
- **Cache Key:** `product:all`

### Error Scenarios

| Error | HTTP Code | Response |
|-------|-----------|----------|
| No JWT token | 401 | `{"error": "Unauthorized", "message": "Full authentication required"}` |
| Invalid token | 401 | `{"error": "Unauthorized", "message": "Invalid JWT token"}` |
| Service down | 503 | `{"error": "Service Unavailable", "message": "Product service unavailable"}` |
| MongoDB down | 500 | `{"error": "Internal Server Error", "message": "Database connection failed"}` |

---

## Scenario 2: Check Inventory Availability

**Business Goal:** Customer checks if product is in stock before ordering

### Flow Map

```mermaid
sequenceDiagram
    participant Customer
    participant Gateway as API Gateway<br/>:8080
    participant Inventory as Inventory Service<br/>:8082
    participant Redis
    participant MySQL

    Customer->>Gateway: GET /api/inventory?skuCode=IPHONE_15_PRO&skuCode=SAMSUNG_S24
    Gateway->>Gateway: Validate JWT
    Gateway->>Inventory: GET /api/inventory?skuCode=...
    Inventory->>Redis: GET inventory:IPHONE_15_PRO
    
    alt Cache HIT
        Redis-->>Inventory: Cached quantity
        Inventory-->>Gateway: 200 OK + In Stock
    else Cache MISS
        Redis-->>Inventory: null
        Inventory->>MySQL: SELECT * FROM inventory WHERE sku_code IN (...)
        MySQL-->>Inventory: Inventory records
        Inventory->>Redis: SET inventory:IPHONE_15_PRO
        Inventory-->>Gateway: 200 OK + In Stock
    end
    
    Gateway-->>Customer: Stock status
```

### Technical Details

| Step | Component | Endpoint/Action | Request | Response |
|------|-----------|----------------|---------|----------|
| 1 | Customer | Check stock | `GET http://localhost:8080/api/inventory?skuCode=IPHONE_15_PRO` | - |
| 2 | API Gateway | JWT validation | Header: `Authorization: Bearer <token>` | - |
| 3 | API Gateway | Route to service | Forwards to `http://inventory-service:8082/api/inventory` | - |
| 4 | Inventory Service | Check Redis cache | Redis: `GET inventory:IPHONE_15_PRO` | - |
| 5 | Inventory Service | Query MySQL (if cache miss) | MySQL: `SELECT * FROM inventory WHERE sku_code = 'IPHONE_15_PRO'` | - |
| 6 | Inventory Service | Update cache | Redis: `SET inventory:IPHONE_15_PRO 100 EX 900` | - |
| 7 | Inventory Service | Return results | - | HTTP 200, JSON array |

### Exact REST Endpoint

```http
GET /api/inventory?skuCode=IPHONE_15_PRO&skuCode=SAMSUNG_S24 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

### Response Example

```json
[
  {
    "skuCode": "IPHONE_15_PRO",
    "isInStock": true,
    "availableQuantity": 100
  },
  {
    "skuCode": "SAMSUNG_S24",
    "isInStock": true,
    "availableQuantity": 150
  }
]
```

### Events: None (Read-only operation)

### Cache Behavior

- **L1 Cache (Caffeine):** Per-SKU cache for 2 minutes
- **L2 Cache (Redis):** Per-SKU cache for 15 minutes with TTL
- **Cache Key Pattern:** `inventory:{skuCode}`
- **Cache Eviction:** On inventory update (write-through)

### Error Scenarios

| Error | HTTP Code | Response |
|-------|-----------|----------|
| Missing skuCode param | 400 | `{"error": "Bad Request", "message": "skuCode parameter is required"}` |
| SKU not found | 200 | `[{"skuCode": "UNKNOWN", "isInStock": false, "availableQuantity": 0}]` |
| Redis down | 200 | Falls back to MySQL (slower) |
| MySQL down | 503 | `{"error": "Service Unavailable", "message": "Database unavailable"}` |

---

## Scenario 3: Place Order (Synchronous + Event-Driven)

**Business Goal:** Customer places order for products, system validates inventory, creates order, and sends notification

### Flow Map

```mermaid
sequenceDiagram
    participant Customer
    participant Gateway as API Gateway<br/>:8080
    participant Order as Order Service<br/>:8083
    participant Inventory as Inventory Service<br/>:8082
    participant MySQL as MySQL<br/>(Order DB)
    participant Kafka
    participant Notification as Notification Service<br/>:8084

    Customer->>Gateway: POST /api/order + Order JSON
    Gateway->>Gateway: Validate JWT + Roles
    Gateway->>Order: POST /api/order
    
    Order->>Order: Validate request (NotBlank, Positive, etc.)
    Order->>Order: Generate order number (UUID)
    
    Note over Order,Inventory: Synchronous inventory check with Circuit Breaker
    Order->>Inventory: GET /api/inventory?skuCode=IPHONE_15_PRO (WebClient)
    
    alt Inventory Available
        Inventory-->>Order: isInStock: true
        Order->>MySQL: BEGIN TRANSACTION
        Order->>MySQL: INSERT INTO orders ...
        Order->>MySQL: INSERT INTO order_line_items ...
        Order->>MySQL: COMMIT
        
        Note over Order,Kafka: Publish OrderPlacedEvent asynchronously
        Order->>Kafka: Publish to 'notificationTopic'
        Order-->>Gateway: 201 Created + Order details
        Gateway-->>Customer: Order confirmation
        
        Note over Kafka,Notification: Asynchronous notification
        Kafka->>Notification: OrderPlacedEvent
        Notification->>Notification: Check idempotency (ProcessedEvent)
        Notification->>Notification: Select notification strategy (Email)
        Notification->>Notification: Send email notification
        Notification->>Notification: Mark event as processed
    else Inventory NOT Available
        Inventory-->>Order: isInStock: false
        Order-->>Gateway: 400 Bad Request
        Gateway-->>Customer: "Product not in stock"
    else Inventory Service Down (Circuit Breaker OPEN)
        Order->>Order: Circuit Breaker triggers
        Order-->>Gateway: 503 Service Unavailable
        Gateway-->>Customer: "Inventory service temporarily unavailable"
    end
```

### Technical Details

| Step | Component | Endpoint/Action | Request | Response |
|------|-----------|----------------|---------|----------|
| 1 | Customer | Submit order | `POST http://localhost:8080/api/order` | - |
| 2 | API Gateway | JWT + role check | Header: `Authorization: Bearer <token>` | - |
| 3 | API Gateway | Route to service | Forwards to `http://order-service:8083/api/order` | - |
| 4 | Order Service | Validate DTO | `@Valid OrderRequest` (Bean Validation) | - |
| 5 | Order Service | Generate order number | UUID-based: `ORDER-a1b2c3d4-e5f6-7890-abcd-ef1234567890` | - |
| 6 | Order Service | Check inventory | `GET http://inventory-service:8082/api/inventory?skuCode=...` | - |
| 7 | Order Service | Create order (if in stock) | MySQL: `INSERT INTO orders (order_number) VALUES (?)` | - |
| 8 | Order Service | Create line items | MySQL: `INSERT INTO order_line_items (order_id, sku_code, ...) VALUES (?, ?, ...)` | - |
| 9 | Order Service | Publish event | Kafka: `Send(notificationTopic, OrderPlacedEvent)` | - |
| 10 | Order Service | Return response | - | HTTP 201, order details JSON |
| 11 | Kafka | Deliver event | Topic: `notificationTopic`, Partition: auto | - |
| 12 | Notification Service | Consume event | `@KafkaListener` receives `OrderPlacedEvent` | - |
| 13 | Notification Service | Check idempotency | Query: `SELECT * FROM processed_event WHERE event_id = ?` | - |
| 14 | Notification Service | Send notification | Strategy pattern: `EmailNotificationStrategy.send()` | - |
| 15 | Notification Service | Mark processed | `INSERT INTO processed_event (event_id, processed_at) VALUES (?, ?)` | - |

### Exact REST Endpoint

```http
POST /api/order HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
Accept: application/json

{
  "orderLineItemDtoList": [
    {
      "skuCode": "IPHONE_15_PRO",
      "price": 999.99,
      "quantity": 1
    },
    {
      "skuCode": "AIRPODS_PRO",
      "price": 249.99,
      "quantity": 2
    }
  ]
}
```

### Response Example (Success)

```json
{
  "orderNumber": "ORDER-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "message": "Order placed successfully",
  "status": "CREATED",
  "orderLineItems": [
    {
      "skuCode": "IPHONE_15_PRO",
      "price": 999.99,
      "quantity": 1
    },
    {
      "skuCode": "AIRPODS_PRO",
      "price": 249.99,
      "quantity": 2
    }
  ],
  "totalAmount": 1499.97
}
```

### Kafka Event Published

**Topic:** `notificationTopic`  
**Event Type:** `OrderPlacedEvent`  
**Schema:**

```json
{
  "eventId": "evt-123e4567-e89b-12d3-a456-426614174000",
  "eventType": "ORDER_PLACED",
  "eventVersion": "1.0",
  "timestamp": "2026-09-08T14:30:00.123Z",
  "correlationId": "corr-98765432-1234-5678-90ab-cdef01234567",
  "payload": {
    "orderNumber": "ORDER-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "customerEmail": "customer@example.com",
    "orderLineItems": [
      {
        "skuCode": "IPHONE_15_PRO",
        "quantity": 1,
        "price": 999.99
      }
    ],
    "totalAmount": 999.99
  },
  "metadata": {
    "source": "order-service",
    "userId": "user-12345",
    "ipAddress": "192.168.1.100"
  }
}
```

### Error Scenarios

| Error | HTTP Code | Response | Reason |
|-------|-----------|----------|--------|
| Missing required fields | 400 | `{"error": "Validation failed", "details": ["skuCode must not be blank"]}` | Bean Validation |
| Invalid quantity (<=0) | 400 | `{"error": "Validation failed", "details": ["quantity must be positive"]}` | Bean Validation |
| Product not in stock | 400 | `{"error": "Bad Request", "message": "Product IPHONE_15_PRO is not in stock"}` | Business rule |
| Inventory service down | 503 | `{"error": "Service Unavailable", "message": "Inventory service temporarily unavailable. Please try again."}` | Circuit Breaker OPEN |
| Database error | 500 | `{"error": "Internal Server Error", "message": "Order creation failed"}` | MySQL exception |
| Kafka publishing failed | 201 | Order created but notification may be delayed | Async failure (order still succeeds) |

### Resilience Patterns Applied

1. **Circuit Breaker (Resilience4j)**
   - Configuration: 50% failure threshold, 5 slow call threshold
   - Fallback: Return 503 with user-friendly message
   - State transitions: CLOSED → OPEN → HALF_OPEN

2. **Retry (Resilience4j)**
   - Max attempts: 3
   - Wait duration: 500ms
   - Exponential backoff: true

3. **Timeout (Resilience4j)**
   - Timeout duration: 3 seconds
   - Prevents hanging calls

4. **Idempotency (Custom)**
   - Event ID tracking in `processed_event` table
   - Prevents duplicate notifications

---

## Scenario 4: Receive Order Notification

**Business Goal:** Customer receives email confirmation after successful order placement

### Flow Map

```mermaid
sequenceDiagram
    participant Kafka
    participant Notification as Notification Service<br/>:8084
    participant DB as MySQL<br/>(Processed Events)
    participant EmailProvider as Email Service<br/>(Future: SendGrid/SES)

    Kafka->>Notification: OrderPlacedEvent
    Notification->>DB: SELECT * FROM processed_event WHERE event_id = ?
    
    alt Event Already Processed
        DB-->>Notification: Record found
        Notification->>Notification: Log: "Event already processed, skipping"
        Note over Notification: Idempotent consumer prevents duplicates
    else New Event
        DB-->>Notification: No record
        Notification->>Notification: Strategy: EmailNotificationStrategy
        Notification->>Notification: Build email content
        
        Note over Notification,EmailProvider: Future integration point
        Notification->>EmailProvider: Send email (placeholder)
        EmailProvider-->>Notification: Success (simulated)
        
        Notification->>DB: INSERT INTO processed_event (event_id, event_type, processed_at)
        DB-->>Notification: Event marked as processed
        Notification->>Notification: Log: "Notification sent successfully"
    end
```

### Technical Details

| Step | Component | Endpoint/Action | Request | Response |
|------|-----------|----------------|---------|----------|
| 1 | Kafka | Deliver event | Consumer group: `notificationGroup` | - |
| 2 | Notification Service | Consume event | `@KafkaListener` method triggered | - |
| 3 | Notification Service | Check idempotency | `SELECT * FROM processed_event WHERE event_id = ?` | - |
| 4 | Notification Service | Select strategy | `NotificationStrategyFactory.getStrategy(EMAIL)` | - |
| 5 | Email Strategy | Build email | Template: "Order {orderNumber} confirmed" | - |
| 6 | Email Strategy | Send email | **Future:** SendGrid/AWS SES integration | - |
| 7 | Notification Service | Mark processed | `INSERT INTO processed_event (...)` | - |

### Kafka Consumer Configuration

```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      group-id: notificationGroup
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

### Event Processing

**Listener Method:**
```java
@KafkaListener(topics = "notificationTopic")
public void handleOrderPlacedEvent(OrderPlacedEvent event) {
    // Implementation in NotificationService
}
```

### Email Content (Placeholder)

```text
Subject: Order Confirmation - ORDER-a1b2c3d4-e5f6-7890-abcd-ef1234567890

Dear Customer,

Your order has been placed successfully!

Order Number: ORDER-a1b2c3d4-e5f6-7890-abcd-ef1234567890
Order Date: 2026-09-08 14:30:00

Order Items:
- iPhone 15 Pro x 1 @ $999.99
- AirPods Pro x 2 @ $249.99

Total: $1,499.97

Thank you for your order!

StockXpress Team
```

### External Integrations (Future Work)

| Integration | Status | Purpose | API Endpoint |
|-------------|--------|---------|-------------|
| **SendGrid** | ⏳ Planned | Email delivery | `POST https://api.sendgrid.com/v3/mail/send` |
| **AWS SES** | ⏳ Planned | Email delivery | `POST https://email.us-east-1.amazonaws.com/` |
| **Twilio** | ⏳ Planned | SMS notifications | `POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json` |
| **Firebase** | ⏳ Planned | Push notifications | `POST https://fcm.googleapis.com/fcm/send` |
| **Stripe** | ⏳ Planned | Payment processing | `POST https://api.stripe.com/v1/payment_intents` |
| **ShipStation** | ⏳ Planned | Shipping fulfillment | `POST https://ssapi.shipstation.com/orders/createorder` |

**Note:** All external integrations are clearly labeled as **future work** and not currently implemented.

### Error Handling

| Error | Action | Retry Strategy |
|-------|--------|----------------|
| Duplicate event | Skip processing (idempotency check) | No retry |
| Email send failure | Log error, mark as failed | Kafka retry (3 attempts) |
| Database unavailable | Exception, Kafka redelivery | Kafka retry with backoff |
| Deserialization error | Dead letter queue | Manual intervention |

---

## Summary Matrix

| Scenario | REST Endpoint | HTTP Method | Auth Required | Events Published | Events Consumed | Database Operations |
|----------|---------------|-------------|---------------|------------------|-----------------|--------------------|
| **Browse Catalog** | `/api/product` | GET | Yes (JWT) | None | None | MongoDB: SELECT |
| **Check Inventory** | `/api/inventory?skuCode={sku}` | GET | Yes (JWT) | None | None | MySQL: SELECT (with Redis cache) |
| **Place Order** | `/api/order` | POST | Yes (JWT + Role) | `OrderPlacedEvent` → `notificationTopic` | None | MySQL: INSERT (orders + line_items) |
| **Receive Notification** | N/A (Event-driven) | N/A | N/A | None | `OrderPlacedEvent` from `notificationTopic` | MySQL: INSERT (processed_event) |

---

## Testing the Flows

### Prerequisites

```bash
# 1. Start all services
docker-compose up -d

# 2. Get access token from Keycloak
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" | jq -r '.access_token')
```

### Test Scenario 1: Browse Catalog

```bash
curl -X GET http://localhost:8080/api/product \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/json" | jq
```

### Test Scenario 2: Check Inventory

```bash
curl -X GET "http://localhost:8080/api/inventory?skuCode=IPHONE_15_PRO&skuCode=SAMSUNG_S24" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/json" | jq
```

### Test Scenario 3: Place Order (Full Flow)

```bash
# Place order
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderLineItemDtoList": [
      {
        "skuCode": "IPHONE_15_PRO",
        "price": 999.99,
        "quantity": 1
      }
    ]
  }')

echo $ORDER_RESPONSE | jq

# Extract order number
ORDER_NUMBER=$(echo $ORDER_RESPONSE | jq -r '.orderNumber')

# Verify order in database
docker exec mysql-order mysql -uroot -ppassword \
  -e "SELECT * FROM order_service.orders WHERE order_number = '$ORDER_NUMBER';"

# Check Kafka for event
docker exec broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic notificationTopic \
  --from-beginning \
  --max-messages 1 \
  --timeout-ms 5000

# Verify notification was processed
docker-compose logs notification-service | grep "Notification sent successfully"
```

### Test Scenario 4: Verify Tracing

```bash
# Open Zipkin
open http://localhost:9411

# Search for recent traces
# Filter by service: "order-service"
# Click on a trace to see full flow:
#   Gateway → Order Service → Inventory Service → Kafka
```

---

## Observability

### Metrics (Prometheus)

```bash
# Order placement rate
rate(http_server_requests_seconds_count{uri="/api/order",method="POST"}[5m])

# Inventory check latency (p95)
histogram_quantile(0.95, http_server_requests_seconds_bucket{uri="/api/inventory"})

# Circuit breaker state
resilience4j_circuitbreaker_state{name="inventoryService"}

# Kafka consumer lag
kafka_consumer_lag{group="notificationGroup",topic="notificationTopic"}
```

### Dashboards (Grafana)

- **Microservices Overview:** `http://localhost:3000/d/microservices-overview`
- **Order Processing:** `http://localhost:3000/d/order-processing`

### Logs (Structured JSON with Correlation IDs)

```bash
# Follow order flow by correlation ID
docker-compose logs -f | grep "corr-98765432-1234-5678-90ab-cdef01234567"

# Filter by service
docker-compose logs order-service | grep "ORDER_PLACED"
```

---

## Architecture Patterns Summary

| Pattern | Implementation | Scenario |
|---------|----------------|----------|
| **API Gateway** | Spring Cloud Gateway | All scenarios (single entry point) |
| **Service Discovery** | Netflix Eureka | Service-to-service communication |
| **Circuit Breaker** | Resilience4j | Order → Inventory communication |
| **Retry** | Resilience4j | Transient failures |
| **Timeout** | Resilience4j | Prevent hanging calls |
| **Event-Driven** | Kafka | Order → Notification communication |
| **Idempotent Consumer** | Processed Event Table | Notification deduplication |
| **Strategy Pattern** | NotificationStrategy | Multiple notification types |
| **Two-Tier Caching** | Caffeine + Redis | Product & Inventory queries |
| **Distributed Tracing** | Zipkin + Sleuth | End-to-end request tracking |
| **Structured Logging** | MDC + Correlation IDs | Log correlation across services |

---

**Document Version:** 1.0  
**Last Updated:** September 8, 2026  
**Maintained By:** StockXpress Development Team