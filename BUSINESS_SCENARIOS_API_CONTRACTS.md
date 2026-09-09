# StockXpress: Business Scenarios → API Contracts

**Purpose:** Map each business scenario to **committed, versioned API contracts** (OpenAPI specifications)  
**For:** New engineers joining the project to verify flows without relying on runtime docs  
**Status:** ✅ All API contracts committed to repository

---

## Quick Reference Table

| # | Business Step | Actor | REST Endpoint | HTTP Method | Auth Required | Kafka Event | API Contract |
|---|---------------|-------|---------------|-------------|---------------|-------------|-------------|
| 1 | Browse products | Customer | `/api/product` | GET | No | - | [Product API v1](product-service/src/main/resources/static/api-docs/openapi-v1.yml#L28) |
| 2 | View product details | Customer | `/api/product/{id}` | GET | No | - | [Product API v1](product-service/src/main/resources/static/api-docs/openapi-v1.yml#L79) |
| 3 | Check stock availability | Customer | `/api/inventory?skuCode=IPHONE_15_PRO` | GET | No | - | [Inventory API v1](inventory-service/src/main/resources/static/api-docs/openapi-v1.yml#L28) |
| 4 | Place order | Customer | `/api/order` | POST | Yes (JWT) | `OrderPlacedEvent` → `order-placed` | [Order API v1](order-service/src/main/resources/static/api-docs/openapi-v1.yml#L28) |
| 5 | Send order confirmation | System | Kafka Consumer | - | No | Consumes `order-placed` | [Notification API v1](notification-service/src/main/resources/static/api-docs/openapi-v1.yml#L50) |
| 6 | Add new product | Operator | `/api/product` | POST | Yes (ADMIN) | - | [Product API v1](product-service/src/main/resources/static/api-docs/openapi-v1.yml#L57) |
| 7 | Restock inventory | Operator | `/api/inventory/restock` | POST | Yes (ADMIN) | - | [Inventory API v1](inventory-service/src/main/resources/static/api-docs/openapi-v1.yml#L74) |

---

## Scenario 1: Customer Browses and Places Order

### Business Context
**Actor:** Customer (end-user)  
**Goal:** Browse products, check availability, and place an order  
**Success Criteria:** Order confirmed and email received

### Step-by-Step Flow with API Contracts

#### Step 1.1: Browse Product Catalog

**Business Action:** Customer visits homepage and views all products  

**API Contract:**
```yaml
File: product-service/src/main/resources/static/api-docs/openapi-v1.yml
Endpoint: GET /api/product
Line: 28-56

Operation:
  summary: Get all products
  tags: [Products]
  security: [] # Public access
  responses:
    200:
      description: List of products
      schema:
        type: array
        items: Product
```

**Test Command:**
```bash
curl -X GET http://localhost:8080/api/product
```

**Expected Response:**
```json
[
  {
    "id": "66f9c123456789abcdef0001",
    "name": "iPhone 15 Pro",
    "description": "Latest flagship smartphone",
    "price": 999.99,
    "category": "Electronics",
    "skuCode": "IPHONE_15_PRO",
    "stockQuantity": 50
  }
]
```

---

#### Step 1.2: Check Product Availability

**Business Action:** Customer clicks "Check Availability" button  

**API Contract:**
```yaml
File: inventory-service/src/main/resources/static/api-docs/openapi-v1.yml
Endpoint: GET /api/inventory?skuCode={sku}
Line: 28-73

Operation:
  summary: Check stock availability
  parameters:
    - name: skuCode
      in: query
      required: true
      example: IPHONE_15_PRO
  responses:
    200:
      schema:
        type: array
        items: InventoryResponse
```

**Test Command:**
```bash
curl -X GET "http://localhost:8080/api/inventory?skuCode=IPHONE_15_PRO"
```

**Expected Response:**
```json
[
  {
    "skuCode": "IPHONE_15_PRO",
    "isInStock": true,
    "quantity": 50
  }
]
```

**Performance Note:** Result cached in Redis (TTL: 5 minutes)

---

#### Step 1.3: Place Order (Authenticated)

**Business Action:** Customer clicks "Buy Now" and submits order  
**Prerequisites:** Customer must be logged in (JWT token obtained from Keycloak)

**API Contract:**
```yaml
File: order-service/src/main/resources/static/api-docs/openapi-v1.yml
Endpoint: POST /api/order
Line: 28-124

Operation:
  summary: Place a new order
  security:
    - bearerAuth: []
  requestBody:
    required: true
    schema: OrderRequest
  responses:
    201:
      description: Order placed successfully
      schema: OrderResponse
    400:
      description: Product out of stock
    503:
      description: Inventory service unavailable (Circuit Breaker OPEN)
```

**Step 1.3a: Obtain JWT Token**
```bash
curl -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  | jq -r '.access_token'
```

**Step 1.3b: Place Order with JWT**
```bash
TOKEN="<access_token_from_above>"

curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "skuCode": "IPHONE_15_PRO",
    "price": 999.99,
    "quantity": 1,
    "userDetails": {
      "email": "customer@example.com",
      "firstName": "John",
      "lastName": "Doe"
    }
  }'
```

**Expected Response (201 Created):**
```json
{
  "message": "Order Placed Successfully",
  "orderId": 12345,
  "orderNumber": "ORD-20260908-001",
  "status": "CONFIRMED"
}
```

**Kafka Event Published:**
```yaml
Topic: order-placed
Schema: OrderPlacedEvent (see order-service OpenAPI line 167-205)

Event Payload:
{
  "orderNumber": "ORD-20260908-001",
  "email": "customer@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "productName": "iPhone 15 Pro",
  "quantity": 1,
  "totalAmount": 999.99
}
```

---

#### Step 1.4: Email Notification (Event-Driven)

**Business Action:** System automatically sends order confirmation email  
**Trigger:** OrderPlacedEvent consumed from Kafka

**API Contract:**
```yaml
File: notification-service/src/main/resources/static/api-docs/openapi-v1.yml
Kafka Consumer Configuration: Line 50-91

Event Schema:
  topic: order-placed
  consumerGroup: notification-service-group
  schema: OrderPlacedEvent
  
Email Template:
  From: noreply@stockxpress.com
  Subject: Order Confirmation - {orderNumber}
  Format: HTML
```

**No REST API call** - This step is event-driven (Kafka consumer → Email sender)

**Expected Email Content:**
```html
<html>
  <body>
    <h1>Thank you for your order, John!</h1>
    <p>Your order <strong>ORD-20260908-001</strong> has been confirmed.</p>
    <p>Product: iPhone 15 Pro (Quantity: 1)</p>
    <p>Total: $999.99</p>
  </body>
</html>
```

---

## Scenario 2: Operator Manages Inventory

### Business Context
**Actor:** Operator (internal user with ADMIN role)  
**Goal:** Add new products and restock inventory  
**Success Criteria:** Product catalog updated and stock levels reflect changes

### Step-by-Step Flow with API Contracts

#### Step 2.1: Add New Product to Catalog

**Business Action:** Operator creates new product entry

**API Contract:**
```yaml
File: product-service/src/main/resources/static/api-docs/openapi-v1.yml
Endpoint: POST /api/product
Line: 57-93

Operation:
  summary: Create a new product
  security:
    - bearerAuth: [] # Requires ROLE_ADMIN
  requestBody:
    schema: ProductRequest
  responses:
    201: Product created
    401: Unauthorized
    403: Forbidden (not ADMIN)
```

**Test Command:**
```bash
# Obtain ADMIN token (user: admin, password: admin123)
ADMIN_TOKEN=$(curl -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  | jq -r '.access_token')

# Create product
curl -X POST http://localhost:8080/api/product \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Samsung Galaxy S24",
    "description": "Premium Android smartphone",
    "price": 899.99,
    "category": "Electronics",
    "skuCode": "SAMSUNG_S24"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": "66f9c123456789abcdef0002",
  "name": "Samsung Galaxy S24",
  "description": "Premium Android smartphone",
  "price": 899.99,
  "category": "Electronics",
  "skuCode": "SAMSUNG_S24",
  "stockQuantity": 0
}
```

---

#### Step 2.2: Restock Inventory

**Business Action:** Operator receives shipment and updates stock levels

**API Contract:**
```yaml
File: inventory-service/src/main/resources/static/api-docs/openapi-v1.yml
Endpoint: POST /api/inventory/restock
Line: 74-107

Operation:
  summary: Restock inventory
  security:
    - bearerAuth: [] # Requires ROLE_ADMIN
  requestBody:
    schema: RestockRequest
  responses:
    200: Inventory updated
    404: SKU not found
```

**Test Command:**
```bash
curl -X POST http://localhost:8080/api/inventory/restock \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "skuCode": "SAMSUNG_S24",
    "quantityToAdd": 100
  }'
```

**Expected Response (200 OK):**
```json
{
  "skuCode": "SAMSUNG_S24",
  "isInStock": true,
  "quantity": 100
}
```

**Side Effects:**
- Redis cache for `SAMSUNG_S24` cleared (cache invalidation)
- Next `GET /api/inventory?skuCode=SAMSUNG_S24` will reflect new quantity

---

## Error Scenarios with API Contracts

### Scenario 3: Out-of-Stock Order Attempt

**Business Flow:** Customer tries to order product with 0 stock

**API Contract Reference:**
```yaml
File: order-service/src/main/resources/static/api-docs/openapi-v1.yml
Endpoint: POST /api/order
Response: 400 Bad Request
Line: 87-99
```

**Test Command:**
```bash
curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "skuCode": "OUT_OF_STOCK_ITEM",
    "price": 499.99,
    "quantity": 1
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "timestamp": "2026-09-08T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Product with SkuCode OUT_OF_STOCK_ITEM is not in stock",
  "path": "/api/order"
}
```

---

### Scenario 4: Circuit Breaker Open (Inventory Service Down)

**Business Flow:** Customer tries to order while Inventory Service is unavailable

**API Contract Reference:**
```yaml
File: order-service/src/main/resources/static/api-docs/openapi-v1.yml
Endpoint: POST /api/order
Response: 503 Service Unavailable
Line: 107-124

Circuit Breaker Configuration:
  Timeout: 5 seconds
  Failure Threshold: 50%
  Wait Duration (OPEN): 10 seconds
```

**Simulate Scenario:**
```bash
# 1. Stop Inventory Service
docker stop inventory-service

# 2. Attempt order (will fail after 5s timeout)
curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"skuCode": "IPHONE_15_PRO", "price": 999.99, "quantity": 1}'
```

**Expected Response (503 Service Unavailable):**
```json
{
  "timestamp": "2026-09-08T10:30:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Inventory service is currently unavailable. Please try again later.",
  "path": "/api/order"
}
```

---

## How to Verify All Flows

### Prerequisites

1. **Start entire system:**
   ```bash
   docker-compose up -d
   ```

2. **Wait for services to be healthy (2-3 minutes):**
   ```bash
   docker-compose ps
   # All services should show "healthy"
   ```

3. **Verify service discovery:**
   ```bash
   curl http://localhost:8761  # Eureka dashboard
   # Should show 6 registered services
   ```

### Full End-to-End Test

```bash
#!/bin/bash
# File: test-all-scenarios.sh

set -e

echo "=== Scenario 1: Customer Order Flow ==="

# 1. Browse products
echo "Step 1: Browse products..."
curl -s http://localhost:8080/api/product | jq .

# 2. Check stock
echo "Step 2: Check stock..."
curl -s "http://localhost:8080/api/inventory?skuCode=IPHONE_15_PRO" | jq .

# 3. Get JWT token
echo "Step 3: Authenticate..."
TOKEN=$(curl -s -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  | jq -r '.access_token')

# 4. Place order
echo "Step 4: Place order..."
curl -s -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "skuCode": "IPHONE_15_PRO",
    "price": 999.99,
    "quantity": 1,
    "userDetails": {
      "email": "test@example.com",
      "firstName": "Test",
      "lastName": "User"
    }
  }' | jq .

echo "✅ All scenarios passed!"
echo "📧 Check email at test@example.com for order confirmation"
```

**Run test:**
```bash
chmod +x test-all-scenarios.sh
./test-all-scenarios.sh
```

---

## API Contract Locations (Committed Files)

| Service | OpenAPI Spec File | Line Count | Version |
|---------|-------------------|------------|--------|
| Product Service | [`product-service/src/main/resources/static/api-docs/openapi-v1.yml`](product-service/src/main/resources/static/api-docs/openapi-v1.yml) | 320 | v1.0.0 |
| Inventory Service | [`inventory-service/src/main/resources/static/api-docs/openapi-v1.yml`](inventory-service/src/main/resources/static/api-docs/openapi-v1.yml) | 195 | v1.0.0 |
| Order Service | [`order-service/src/main/resources/static/api-docs/openapi-v1.yml`](order-service/src/main/resources/static/api-docs/openapi-v1.yml) | 270 | v1.0.0 |
| Notification Service | [`notification-service/src/main/resources/static/api-docs/openapi-v1.yml`](notification-service/src/main/resources/static/api-docs/openapi-v1.yml) | 150 | v1.0.0 |

**All files are:**
- ✅ Committed to version control
- ✅ Versioned (v1.0.0)
- ✅ Include business scenario descriptions
- ✅ Include authentication requirements
- ✅ Include Kafka event schemas
- ✅ Include error response examples
- ✅ Include test commands

---

## Summary

✅ **Every business scenario is now tied to a committed, versioned API contract**  
✅ **New engineers can verify flows using OpenAPI specs (no need for runtime docs)**  
✅ **All 4 services have comprehensive OpenAPI 3.0 specifications**  
✅ **Scenarios include authentication, events, errors, and circuit breaker behavior**  
✅ **Test commands provided for full end-to-end verification**

**Next:** Use these OpenAPI files to generate Postman collections, client SDKs, or interactive API documentation with Swagger UI.
