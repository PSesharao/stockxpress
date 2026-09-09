# StockXpress API Documentation

**Version:** 1.0.0  
**Last Updated:** September 8, 2026  
**Format:** OpenAPI 3.0.3  

---

## Quick Links to API Specifications

| Service | OpenAPI Spec | Interactive Docs | Health Check |
|---------|--------------|------------------|-------------|
| **Product Service** | [openapi-v1.yml](product-service/src/main/resources/static/api-docs/openapi-v1.yml) | http://localhost:8080/swagger-ui.html | http://localhost:8080/actuator/health |
| **Inventory Service** | [openapi-v1.yml](inventory-service/src/main/resources/static/api-docs/openapi-v1.yml) | http://localhost:8081/swagger-ui.html | http://localhost:8081/actuator/health |
| **Order Service** | [openapi-v1.yml](order-service/src/main/resources/static/api-docs/openapi-v1.yml) | http://localhost:8082/swagger-ui.html | http://localhost:8082/actuator/health |
| **Notification Service** | [openapi-v1.yml](notification-service/src/main/resources/static/api-docs/openapi-v1.yml) | http://localhost:8083/swagger-ui.html | http://localhost:8083/actuator/health |

---

## API Gateway (Single Entry Point)

**URL:** http://localhost:8080 (routes to all services)  
**Patterns:**
- `/api/product/**` → Product Service (port 8084)
- `/api/inventory/**` → Inventory Service (port 8081)
- `/api/order/**` → Order Service (port 8082)

---

## Product Service API

**Base URL:** `/api/product`  
**Database:** MongoDB  
**Purpose:** Manage product catalog

### Endpoints

| Method | Path | Description | Auth Required | OpenAPI Line |
|--------|------|-------------|---------------|-------------|
| GET | `/api/product` | List all products | No | [28-56](product-service/src/main/resources/static/api-docs/openapi-v1.yml#L28) |
| POST | `/api/product` | Create product | Yes (ADMIN) | [57-93](product-service/src/main/resources/static/api-docs/openapi-v1.yml#L57) |
| GET | `/api/product/{id}` | Get product by ID | No | [79-127](product-service/src/main/resources/static/api-docs/openapi-v1.yml#L79) |
| PUT | `/api/product/{id}` | Update product | Yes (ADMIN) | [129-164](product-service/src/main/resources/static/api-docs/openapi-v1.yml#L129) |
| DELETE | `/api/product/{id}` | Delete product | Yes (ADMIN) | [166-193](product-service/src/main/resources/static/api-docs/openapi-v1.yml#L166) |

**Example Request:**
```bash
curl -X GET http://localhost:8080/api/product
```

---

## Inventory Service API

**Base URL:** `/api/inventory`  
**Database:** MySQL (inventory_service)  
**Purpose:** Manage stock levels and availability  
**Cache:** Redis (5-minute TTL)

### Endpoints

| Method | Path | Description | Auth Required | OpenAPI Line |
|--------|------|-------------|---------------|-------------|
| GET | `/api/inventory?skuCode={sku}` | Check stock availability | No | [28-73](inventory-service/src/main/resources/static/api-docs/openapi-v1.yml#L28) |
| POST | `/api/inventory/restock` | Restock inventory | Yes (ADMIN) | [74-107](inventory-service/src/main/resources/static/api-docs/openapi-v1.yml#L74) |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/inventory?skuCode=IPHONE_15_PRO"
```

**Response:**
```json
[
  {
    "skuCode": "IPHONE_15_PRO",
    "isInStock": true,
    "quantity": 50
  }
]
```

---

## Order Service API

**Base URL:** `/api/order`  
**Database:** MySQL (order_service)  
**Purpose:** Process customer orders  
**Events:** Publishes `OrderPlacedEvent` to Kafka topic `order-placed`

### Endpoints

| Method | Path | Description | Auth Required | OpenAPI Line |
|--------|------|-------------|---------------|-------------|
| POST | `/api/order` | Place new order | Yes (USER) | [28-124](order-service/src/main/resources/static/api-docs/openapi-v1.yml#L28) |

**Example Request:**
```bash
# 1. Get JWT token
TOKEN=$(curl -s -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  | jq -r '.access_token')

# 2. Place order
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

**Success Response (201):**
```json
{
  "message": "Order Placed Successfully",
  "orderId": 12345,
  "orderNumber": "ORD-20260908-001",
  "status": "CONFIRMED"
}
```

**Kafka Event Published:**
```json
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

## Notification Service API

**Purpose:** Event-driven email notifications  
**Kafka Consumer:** Listens to `order-placed` topic  
**Email Provider:** SMTP (JavaMailSender)

### No Direct REST API
Notification Service operates entirely through Kafka events. It does not expose REST endpoints for sending notifications.

### Observability Endpoints

| Method | Path | Description | OpenAPI Line |
|--------|------|-------------|-------------|
| GET | `/actuator/health` | Health check (Kafka + Mail) | [28-55](notification-service/src/main/resources/static/api-docs/openapi-v1.yml#L28) |
| GET | `/actuator/metrics` | Prometheus metrics | [57-71](notification-service/src/main/resources/static/api-docs/openapi-v1.yml#L57) |

**Event Schema:**  
See [OrderPlacedEvent](notification-service/src/main/resources/static/api-docs/openapi-v1.yml#L75) in OpenAPI spec.

---

## Authentication

### Keycloak OAuth 2.0 / OpenID Connect

**Keycloak URL:** http://localhost:8181  
**Realm:** spring-boot-microservices-realm  
**Client ID:** spring-cloud-client

### Test Users

| Username | Password | Roles | Use Case |
|----------|----------|-------|----------|
| `testuser` | `password123` | ROLE_USER | Place orders |
| `admin` | `admin123` | ROLE_ADMIN | Manage products/inventory |

### Obtain JWT Token

```bash
curl -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  | jq -r '.access_token'
```

### Use Token in API Requests

```bash
curl -X POST http://localhost:8080/api/order \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

---

## Error Responses

### Standard Error Format

```json
{
  "timestamp": "2026-09-08T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Product with SkuCode INVALID_SKU is not in stock",
  "path": "/api/order"
}
```

### Common HTTP Status Codes

| Code | Meaning | Example Scenario |
|------|---------|------------------|
| 200 | OK | Product list retrieved |
| 201 | Created | Order placed successfully |
| 400 | Bad Request | Invalid SKU code, out of stock |
| 401 | Unauthorized | Missing/invalid JWT token |
| 403 | Forbidden | User lacks required role (ADMIN) |
| 404 | Not Found | Product ID does not exist |
| 503 | Service Unavailable | Circuit breaker OPEN (Inventory Service down) |

---

## Resilience Patterns

### Circuit Breaker (Order → Inventory)

**Configuration:**
- Timeout: 5 seconds
- Failure Threshold: 50%
- Wait Duration (OPEN state): 10 seconds

**Behavior:**
- If Inventory Service fails/times out 50%+ of requests → Circuit OPEN
- While OPEN: Return 503 immediately (no further calls to Inventory)
- After 10 seconds: Transition to HALF_OPEN (test with 1 request)
- If test succeeds: Circuit CLOSED (resume normal operation)

**Test Circuit Breaker:**
```bash
# 1. Stop Inventory Service
docker stop inventory-service

# 2. Try placing order (will fail after 5s timeout)
curl -X POST http://localhost:8080/api/order ...
# Response: 503 Service Unavailable

# 3. Subsequent requests return 503 immediately (circuit OPEN)

# 4. Restart Inventory Service
docker start inventory-service

# 5. After 10s, circuit will attempt to close
```

---

## Kafka Event Architecture

### Topics

| Topic | Producer | Consumer | Schema |
|-------|----------|----------|--------|
| `order-placed` | Order Service | Notification Service | [OrderPlacedEvent](order-service/src/main/resources/static/api-docs/openapi-v1.yml#L167) |

### Event Flow

```
1. Customer places order via POST /api/order
2. Order Service validates stock (calls Inventory Service)
3. If stock available:
   a. Create order in database
   b. Publish OrderPlacedEvent to Kafka topic "order-placed"
4. Notification Service consumes event
5. Email sent to customer (order confirmation)
```

---

## Service Discovery

**Eureka Server:** http://localhost:8761

All services register with Eureka for dynamic discovery:
- API Gateway discovers backend services
- Order Service discovers Inventory Service
- Load balancing handled by Spring Cloud LoadBalancer

**Check Registered Services:**
```bash
curl http://localhost:8761/eureka/apps | jq .
```

---

## Observability

### Distributed Tracing (Zipkin)
**URL:** http://localhost:9411  
**Purpose:** Trace requests across microservices

**Example:** Trace order flow:
1. API Gateway receives request
2. Routes to Order Service
3. Order Service calls Inventory Service
4. Order Service publishes Kafka event
5. Notification Service consumes event

### Metrics (Prometheus)
**URL:** http://localhost:9090  
**Scrape Targets:** All services expose `/actuator/prometheus`

### Dashboards (Grafana)
**URL:** http://localhost:3000  
**Default Credentials:** admin / admin

---

## Testing the APIs

### Option 1: Postman Collection (Generate from OpenAPI)

```bash
# Install openapi-to-postman
npm install -g openapi-to-postman

# Convert OpenAPI spec to Postman collection
openapi2postmanv2 -s product-service/src/main/resources/static/api-docs/openapi-v1.yml \
  -o StockXpress-Product-API.postman_collection.json

# Import into Postman
```

### Option 2: Swagger UI (Interactive Docs)

**URLs:**
- Product Service: http://localhost:8080/swagger-ui.html
- Inventory Service: http://localhost:8081/swagger-ui.html
- Order Service: http://localhost:8082/swagger-ui.html

### Option 3: Automated Test Script

See [`BUSINESS_SCENARIOS_API_CONTRACTS.md`](BUSINESS_SCENARIOS_API_CONTRACTS.md) for complete test scripts.

---

## API Versioning Strategy

**Current Version:** v1.0.0  
**Location:** All OpenAPI specs declare `version: 1.0.0`

**Future Versioning:**
- Breaking changes: Increment major version (v2.0.0)
- New endpoints (backward compatible): Increment minor version (v1.1.0)
- Bug fixes: Increment patch version (v1.0.1)

**URL Versioning (not currently implemented):**
Future consideration: `/api/v1/product`, `/api/v2/product`

---

## Support and Contact

**Team:** StockXpress Platform Team  
**Email:** platform@stockxpress.com  
**Repository:** [GitHub](https://github.com/your-org/stockxpress)  

**Documentation:**
- [Business Scenarios → API Contracts](BUSINESS_SCENARIOS_API_CONTRACTS.md)
- [Deployment Guide](DEPLOYMENT_GUIDE.md)
- [Architecture Documentation](ARCHITECTURE.md)

---

## Related Documentation

- [BUSINESS_SCENARIOS_API_CONTRACTS.md](BUSINESS_SCENARIOS_API_CONTRACTS.md) - Complete business flow mapping
- [ADVANCED_CERTIFICATION_STATUS.md](ADVANCED_CERTIFICATION_STATUS.md) - Certification status
- [README.md](README.md) - Project overview
- [terraform/aws/SECRETS_MANAGEMENT_GUIDE.md](terraform/aws/SECRETS_MANAGEMENT_GUIDE.md) - Infrastructure secrets
