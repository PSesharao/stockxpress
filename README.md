# StockXpress - Microservices E-Commerce Platform

[![CI - Build and Test](https://github.com/PSesharao/stockxpress/workflows/CI%20-%20Build%20and%20Test/badge.svg)](https://github.com/PSesharao/stockxpress/actions/workflows/build.yml)
[![CD - Deploy](https://github.com/PSesharao/stockxpress/workflows/CD%20-%20Deploy/badge.svg)](https://github.com/PSesharao/stockxpress/actions/workflows/deploy.yml)
[![Security Scan](https://github.com/PSesharao/stockxpress/workflows/Security%20Scan/badge.svg)](https://github.com/PSesharao/stockxpress/actions/workflows/security-scan.yml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.14-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-8-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)](https://www.docker.com/)

StockXpress is a production-ready microservices-based e-commerce platform built using Spring Boot, demonstrating modern
architectural patterns including service discovery, API gateway, event-driven architecture, distributed tracing, and
OAuth 2.0 security.

---

## 📋 Table of Contents

1. [Overview](#-overview)
2. [Who Uses StockXpress](#-who-uses-stockxpress)
3. [Key Business Scenarios](#-key-business-scenarios)
4. [Architecture](#-architecture)
5. [Technology Stack](#-technology-stack)
6. [Prerequisites](#-prerequisites)
7. [Quick Start Deployment](#-quick-start-deployment)
8. [API Documentation](#-api-documentation)
9. [Troubleshooting](#-troubleshooting)
10. [Architecture Decision Records](#-architecture-decision-records)
11. [Project Structure](#-project-structure)

---

## 🎯 Overview

StockXpress provides a complete e-commerce backend system with the following capabilities:

- **Product Catalog Management** - Browse and manage product inventory
- **Order Processing** - Place and track customer orders
- **Inventory Management** - Real-time stock availability checks
- **Event-Driven Notifications** - Asynchronous order confirmations
- **Distributed Tracing** - Monitor requests across services
- **Secure Authentication** - OAuth 2.0 with Keycloak
- **Service Discovery** - Dynamic service registration and discovery
- **API Gateway** - Single entry point with load balancing

---

## 👥 Who Uses StockXpress

StockXpress is designed for three primary user groups:

### 1. E-Commerce Operators

**Business users managing the platform**

- **Merchandising Teams**: Manage product catalog, pricing, and descriptions
- **Operations Teams**: Monitor inventory levels and order fulfillment
- **Customer Service**: Track order status and customer inquiries

**Key Activities:**

- Add/update products in the catalog
- Monitor inventory availability
- View order processing status
- Manage product pricing

### 2. API Consumers

**External systems integrating with StockXpress**

- **Web Applications**: React/Angular frontends consuming REST APIs
- **Mobile Apps**: iOS/Android apps for customer ordering
- **Third-Party Systems**: Payment gateways, shipping providers, analytics platforms
- **Partner Integrations**: B2B integrations for wholesale ordering

**Integration Methods:**

- REST APIs via API Gateway (port 8080)
- OAuth 2.0 authentication with JWT tokens
- Event-driven integration via Kafka (future)

### 3. Developers & DevOps

**Technical teams building and operating the platform**

- **Backend Developers**: Extend microservices functionality
- **Frontend Developers**: Consume APIs for UI development
- **DevOps Engineers**: Deploy, monitor, and scale the platform
- **QA Engineers**: Test services and integrations

**Technical Tasks:**

- Deploy services using Docker Compose
- Monitor distributed traces in Zipkin
- Configure service discovery with Eureka
- Implement new microservices following established patterns

---

## 🎬 Key Business Scenarios

StockXpress supports the following core e-commerce workflows:

### Scenario 1: Browse Product Catalog

**Actor:** Customer (via Web/Mobile App)  
**Goal:** Discover products and view details

```mermaid
sequenceDiagram
    participant Customer
    participant Gateway as API Gateway
    participant Product as Product Service
    participant Mongo as MongoDB
    
    Customer->>Gateway: GET /api/product
    Gateway->>Product: Forward request
    Product->>Mongo: Query products
    Mongo-->>Product: Return catalog
    Product-->>Gateway: Product list
    Gateway-->>Customer: 200 OK [Products]
```

**API Endpoint:** `GET /api/product`  
**Authentication:** Required (OAuth 2.0)

---

### Scenario 2: Place Order

**Actor:** Customer (via Web/Mobile App)  
**Goal:** Purchase products with inventory validation

```mermaid
sequenceDiagram
    participant Customer
    participant Gateway as API Gateway
    participant Order as Order Service
    participant Inventory as Inventory Service
    participant Kafka
    participant Notification as Notification Service
    
    Customer->>Gateway: POST /api/order
    Gateway->>Order: Place order
    Order->>Inventory: Check stock availability
    Inventory-->>Order: In stock ✓
    Order->>Order: Save order
    Order->>Kafka: Publish OrderPlacedEvent
    Order-->>Gateway: 201 Created
    Gateway-->>Customer: Order confirmation
    Kafka->>Notification: Consume event
    Notification->>Customer: Send email notification
```

**API Endpoint:** `POST /api/order`  
**Authentication:** Required (OAuth 2.0)  
**Business Rules:**

- Maximum 50 line items per order
- Quantity per item: 1-1000
- All SKUs must be in stock

---

### Scenario 3: Check Inventory Availability

**Actor:** Order Service (internal) or External System  
**Goal:** Verify product stock before order placement

**API Endpoint:** `GET /api/inventory?skuCode=SKU-001&skuCode=SKU-002`  
**Response:**

```json
[
  {"skuCode": "SKU-001", "isInStock": true, "quantity": 100},
  {"skuCode": "SKU-002", "isInStock": false, "quantity": 0}
]
```

**Caching:** 10-minute TTL for performance optimization

---

### Scenario 4: Receive Order Notifications

**Actor:** Notification Service (automated)  
**Goal:** Notify customers of successful order placement

**Workflow:**

1. Order Service publishes `OrderPlacedEvent` to Kafka topic
2. Notification Service consumes event
3. Checks idempotency (prevent duplicate notifications)
4. Sends email/SMS to customer
5. Records event as processed

**Event-Driven Benefits:**

- Non-blocking order placement
- Resilient to notification failures
- Scalable notification processing

---

## 🏛️ Architecture

StockXpress follows a microservices architecture with clear service boundaries and well-defined communication patterns.

### Architecture Diagram

![Architecture Diagram](project-architechture.png)

### 📚 Detailed Architecture Documentation

For in-depth understanding of the system architecture, please refer to:

| Document | Description |
|----------|-------------|
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | Complete C4 model diagrams (Context, Container, Component), technology stack, infrastructure components, communication patterns, and deployment architecture |
| **[DOMAIN_MODEL.md](DOMAIN_MODEL.md)** | Domain-driven design documentation including bounded contexts, entities, aggregates, value objects, domain events, and business rules |
| **[SERVICE_BOUNDARIES.md](SERVICE_BOUNDARIES.md)** | Service decomposition rationale, responsibility assignment, API contracts, versioning strategy, and data ownership patterns |

### Architecture Highlights

- **C4 Model**: Comprehensive architecture visualized at Context, Container, and Component levels
- **Domain-Driven Design**: Clear bounded contexts aligned with business capabilities
- **Event-Driven Architecture**: Asynchronous communication via Apache Kafka
- **Microservices Patterns**: Circuit Breaker, Service Discovery, API Gateway, Database per Service
- **Observability**: Distributed tracing with Zipkin, metrics with Prometheus, dashboards with Grafana

---

## Services

### Product Service

- **Description**: Acts as the Product Catalog. Allows for creating and viewing products.

### Order Service

- **Description**: Enables to place orders for products.

### Inventory Service

- **Description**: Used by the Order Service to check product availability before placing an order.

### Notification Service

- **Description**: Sends notifications after successful order placement.

### Discovery Service

- **Description**: Utilizes Netflix Eureka for Dynamic Service Registration, Service Lookup, and Load Balancing. Manages service discovery within the microservices architecture.

### API Gateway Service

- **Description**: Implements API Gateway using Spring Cloud Gateway for routing and load balancing of client requests.

## Features

### Keycloak identity and access management ( IAM )

- **Description**: Keycloak identity and access management ( IAM ) auth server is used to provide support for OAuth 2.0 and OpenID Connect, making it easy to secure APIs and provide identity information to client securely with JWT tokens. 

### Circuit Breaker

- **Description**: Utilizes circuit breakers to provide fault tolerance and prevent cascading failures. Implements fallback mechanisms to ensure system resilience.

### Zipkin

- **Description**: Utilizes Zipkin for distributed tracing, latency monitoring, and error tracing. Provides valuable insights into request processing and system performance.

### Kafka Event-Driven Architecture

- **Description**: Implements event-driven architecture using Kafka. The Order Service produces events stored in Kafka topics, and the Notification Service consumes these events for order notifications.

### Interactions

- **Synchronous Communication**: Order Service communicates synchronously with Inventory Service for product availability checks.
- **Asynchronous Communication**: Order Service communicates asynchronously with the Notification Service for order notifications.

### Dockerized Services

- **Description**: All services are containerized using Docker, facilitating easy deployment and scalability.

## Project Overview

1. KeyCloak Home

![KeyCloak Home](api-responses/key-cloak.png)

2. KeyCloak Create Realm

![KeyCloak Create Realm](api-responses/key-cloak-create-realm.png)

3. KeyCloak Create Client

![KeyCloak Create Client](api-responses/key-cloak-create-client.png)


4. KeyCloak Create Client

![KeyCloak Create Client](api-responses/key-cloak-create-client-1.png)

5. KeyCloak Client Secret

![KeyCloak Client Secret](api-responses/key-cloak-client-secret.png)

6. KeyCloak Access Token

![KeyCloak Access Token](api-responses/key-cloak-access-token.png)

7. Unauthorized Request

![Unauthorized Request](api-responses/unauthorized_request.png)

8. Product Catalogue

![Product Catalogue](api-responses/product-catalogue.png)

9. Out Of Order Stock
![Out Of Order Stock](api-responses/order_out_of_stock.png)

10. Successful Order
![Out Of Order Stock](api-responses/successful_order.png)

11. Eureka Server
![Eureka Server](api-responses/eureka-server.png)


12. Zipkins Trace
![Eureka Server](api-responses/zipkins_trace.png)

---

## 📦 Technology Stack

### Core Framework

| Technology | Version | Purpose |
|-----------|---------|----------|
| **Spring Boot** | 2.7.14 | Core microservices framework |
| **Spring Cloud** | 2021.0.8 | Microservices infrastructure |
| **Java** | 8 | Programming language |
| **Maven** | 3.6+ | Build automation |

### Microservices Infrastructure

| Technology | Version | Purpose |
|-----------|---------|----------|
| **Netflix Eureka** | 2021.0.8 | Service discovery and registration |
| **Spring Cloud Gateway** | 2021.0.8 | API Gateway and routing |
| **Apache Kafka** | 7.0.1 | Event streaming and messaging |
| **Resilience4j** | - | Circuit breaker, retry, timeout |

### Data Persistence

| Technology | Version | Purpose |
|-----------|---------|----------|
| **MySQL** | 8.0 | Relational database (Orders, Inventory, Notifications) |
| **MongoDB** | 4.4.14 | Document database (Product Catalog) |
| **Spring Data JPA** | 2.7.14 | Relational data access |
| **Spring Data MongoDB** | 2.7.14 | MongoDB data access |
| **Redis** | 7-alpine | Distributed caching |

### Security

| Technology | Version | Purpose |
|-----------|---------|----------|
| **Keycloak** | 20.0.0 | Identity and Access Management (IAM) |
| **Spring Security** | 5.7.x | Security framework |
| **OAuth 2.0** | - | Authorization protocol |
| **JWT** | - | Token-based authentication |

### Observability

| Technology | Version | Purpose |
|-----------|---------|----------|
| **Zipkin** | Latest | Distributed tracing |
| **Spring Cloud Sleuth** | 2021.0.8 | Trace ID propagation |
| **Prometheus** | Latest | Metrics collection |
| **Grafana** | Latest | Metrics visualization |
| **Spring Boot Actuator** | 2.7.14 | Health checks and metrics |

### DevOps

| Technology | Version | Purpose |
|-----------|---------|----------|
| **Docker** | 20.10+ | Containerization |
| **Docker Compose** | 3.8 | Multi-container orchestration |
| **Jib** | 3.2.1 | Container image builder |
| **TestContainers** | 1.18.3 | Integration testing |

---

## ⚙️ Prerequisites

Before deploying StockXpress, ensure you have the following installed:

### Required Software

#### 1. Docker & Docker Compose

**Version Required:**

- Docker Engine 20.10 or higher
- Docker Compose 1.29 or higher (v3.8 format support)

**Installation:**

```bash
# Verify Docker installation
docker --version
# Expected output: Docker version 20.10.x or higher

# Verify Docker Compose installation
docker-compose --version
# Expected output: docker-compose version 1.29.x or higher
```

**Installation Guides:**

- [Install Docker Desktop (Windows/Mac)](https://docs.docker.com/desktop/)
- [Install Docker Engine (Linux)](https://docs.docker.com/engine/install/)
- [Install Docker Compose](https://docs.docker.com/compose/install/)

---

#### 2. Java Development Kit (JDK)

**Version Required:** Java 8 or higher

**Installation:**

```bash
# Verify Java installation
java -version
# Expected output: java version "1.8.0_xxx" or higher
```

**Recommended Distributions:**

- [Eclipse Temurin (OpenJDK)](https://adoptium.net/) - Recommended
- [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
- [Amazon Corretto](https://aws.amazon.com/corretto/)

---

#### 3. Apache Maven

**Version Required:** Maven 3.6 or higher

**Installation:**

```bash
# Verify Maven installation
mvn -version
# Expected output: Apache Maven 3.6.x or higher
```

**Installation Guide:**

- [Install Maven](https://maven.apache.org/install.html)

---

### Optional (For Development)

#### 4. Git

```bash
git --version
# Expected output: git version 2.x.x or higher
```

#### 5. IDE (Recommended)

- **IntelliJ IDEA** (Ultimate or Community)
- **Eclipse IDE for Enterprise Java Developers**
- **Visual Studio Code** with Java extensions

---

### System Requirements

**Minimum Hardware:**

- **CPU**: 4 cores
- **RAM**: 8 GB
- **Disk Space**: 10 GB free space

**Recommended Hardware:**

- **CPU**: 8 cores
- **RAM**: 16 GB
- **Disk Space**: 20 GB free space

**Why?** StockXpress runs 6 microservices + 7 infrastructure services (databases, Kafka, Keycloak, etc.) simultaneously.

---

## 🚀 Quick Start Deployment

Follow these steps to deploy StockXpress locally using Docker Compose.

### Step 1: Clone the Repository

```bash
git clone https://github.com/PSesharao/stockxpress.git
cd stockxpress
```

---

### Step 2: Configure Environment Variables

StockXpress uses environment variables for configuration. A template file `.env.example` is provided.

#### Copy the Example File

```bash
cp .env.example .env
```

#### Edit `.env` File (Optional)

Open `.env` in your favorite text editor and customize the values:

```bash
# Example: Change default passwords
nano .env
# or
vim .env
# or
code .env  # If using VS Code
```

#### Key Configuration Variables

**Database Credentials:**

```env
# MongoDB (Product Service)
MONGO_USERNAME=root
MONGO_PASSWORD=change_mongo_password  # CHANGE THIS

# MySQL (Order Service)
ORDER_DB_USERNAME=orderuser
ORDER_DB_PASSWORD=change_order_password  # CHANGE THIS

# MySQL (Inventory Service)
INVENTORY_DB_USERNAME=inventoryuser
INVENTORY_DB_PASSWORD=change_inventory_password  # CHANGE THIS
```

**Keycloak (Authentication):**

```env
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=change_keycloak_admin_password  # CHANGE THIS
```

**Eureka (Service Discovery):**

```env
EUREKA_USERNAME=eureka
EUREKA_PASSWORD=change_this_password  # CHANGE THIS
```

**Redis (Caching):**

```env
REDIS_PASSWORD=change_redis_password  # CHANGE THIS
```

**Grafana (Monitoring):**

```env
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=change_grafana_password  # CHANGE THIS
```

> ⚠️ **Security Warning:** Never commit `.env` file with real credentials to version control!

---

### Step 3: Build Service Docker Images

Build Docker images for all microservices using Maven and Jib:

```bash
# Build all services (this may take 5-10 minutes)
mvn clean compile jib:dockerBuild
```

**What This Does:**

- Compiles Java code for all services
- Runs tests
- Builds Docker images using Jib (no Dockerfile needed)
- Tags images as `stockxpress/<service-name>:latest`

**Expected Output:**

```
[INFO] Built image to Docker daemon as stockxpress/product-service
[INFO] Built image to Docker daemon as stockxpress/order-service
[INFO] Built image to Docker daemon as stockxpress/inventory-service
[INFO] Built image to Docker daemon as stockxpress/notification-service
[INFO] Built image to Docker daemon as stockxpress/discovery-server
[INFO] Built image to Docker daemon as stockxpress/api-gateway
[INFO] BUILD SUCCESS
```

**Verify Images:**

```bash
docker images | grep stockxpress
```

---

### Step 4: Start All Services with Docker Compose

```bash
# Start all services in detached mode
docker-compose up -d
```

**What This Does:**

- Starts all infrastructure services (databases, Kafka, Keycloak, Zipkin, etc.)
- Starts all microservices (Product, Order, Inventory, Notification, API Gateway, Discovery Server)
- Creates Docker network `stockxpress-network`
- Creates persistent volumes for databases

**Startup Time:** ~2-3 minutes for all services to be healthy

---

### Step 5: Verify Deployment

#### Check Service Health

```bash
# View running containers
docker-compose ps

# Expected: All services should be "Up" and "healthy"
```

#### Check Logs

```bash
# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f order-service
```

#### Access Service Dashboards

Once all services are running, access the following URLs:

| Service | URL | Credentials |
|---------|-----|-------------|
| **API Gateway** | http://localhost:8080 | OAuth 2.0 Token Required |
| **Eureka Dashboard** | http://localhost:8761 | `eureka` / `password` (see `.env`) |
| **Zipkin Tracing** | http://localhost:9411 | No authentication |
| **Keycloak Admin** | http://localhost:8181 | `admin` / `admin` (see `.env`) |
| **Prometheus** | http://localhost:9090 | No authentication |
| **Grafana** | http://localhost:3000 | `admin` / `admin` (see `.env`) |

---

### Step 6: Configure Keycloak (First-Time Setup)

Before making API calls, configure Keycloak for OAuth 2.0 authentication.

#### 1. Access Keycloak Admin Console

Open http://localhost:8181 and login with:

- **Username:** `admin`
- **Password:** `admin` (or value from `.env`)

#### 2. Create Realm

1. Click dropdown in top-left (says "Master")
2. Click **"Create Realm"**
3. **Realm name:** `spring-boot-microservices-realm`
4. Click **"Create"**

#### 3. Create Client

1. Navigate to **Clients** → **Create Client**
2. **Client ID:** `spring-cloud-client`
3. **Client Protocol:** `openid-connect`
4. Click **"Next"**
5. **Client authentication:** `ON`
6. **Authorization:** `ON`
7. **Authentication flow:** Enable "Standard flow" and "Direct access grants"
8. Click **"Save"**

#### 4. Get Client Secret

1. Go to **Clients** → `spring-cloud-client` → **Credentials** tab
2. Copy the **Client Secret** (you'll need this for API calls)

---

### Step 7: Get OAuth 2.0 Access Token

Before calling APIs, obtain a JWT token from Keycloak:

```bash
curl -X POST 'http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=spring-cloud-client' \
  -d 'client_secret=YOUR_CLIENT_SECRET_HERE' \
  -d 'grant_type=client_credentials'
```

**Response:**

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

Copy the `access_token` value for use in API calls.

---

### Step 8: Test the System

#### Create a Product

```bash
curl -X POST 'http://localhost:8080/api/product' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN_HERE' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Wireless Mouse",
    "description": "Ergonomic wireless mouse",
    "price": 29.99
  }'
```

#### Get All Products

```bash
curl -X GET 'http://localhost:8080/api/product' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN_HERE'
```

#### Place an Order

```bash
curl -X POST 'http://localhost:8080/api/order' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN_HERE' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderLineItemList": [
      {
        "skuCode": "SKU-001",
        "price": 29.99,
        "quantity": 2
      }
    ]
  }'
```

**Expected Response:**

```json
{
  "message": "Order placed successfully. Order Number: ORD-20240115-001"
}
```

#### Check Inventory

```bash
curl -X GET 'http://localhost:8080/api/inventory?skuCode=SKU-001' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN_HERE'
```

---

### Step 9: Monitor the System

#### View Distributed Traces (Zipkin)

1. Open http://localhost:9411
2. Click **"Run Query"** to see recent traces
3. Click on a trace to see request flow across services

#### View Service Registry (Eureka)

1. Open http://localhost:8761
2. Login with Eureka credentials
3. See all registered services and their instances

#### View Metrics (Prometheus)

1. Open http://localhost:9090
2. Query metrics: `http_server_requests_seconds_count`

#### View Dashboards (Grafana)

1. Open http://localhost:3000
2. Login with Grafana credentials
3. Import Spring Boot dashboards

---

### Step 10: Stop Services

```bash
# Stop all services (keeps data)
docker-compose stop

# Stop and remove all containers (keeps volumes)
docker-compose down

# Stop, remove containers AND delete all data
docker-compose down -v
```

---

## 📚 API Documentation

### Base URL

All API requests go through the API Gateway:

```
http://localhost:8080
```

### Authentication

All endpoints require OAuth 2.0 Bearer token:

```
Authorization: Bearer <access_token>
```

### Available Endpoints

#### Product Service

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|-------------|
| `POST` | `/api/product` | Create a product | `{"name": "string", "description": "string", "price": number}` |
| `GET` | `/api/product` | Get all products | None |

#### Order Service

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|-------------|
| `POST` | `/api/order` | Place an order | `{"orderLineItemList": [{"skuCode": "string", "price": number, "quantity": number}]}` |

#### Inventory Service

| Method | Endpoint | Description | Query Parameters |
|--------|----------|-------------|------------------|
| `GET` | `/api/inventory` | Check stock availability | `skuCode` (multiple allowed) |

### Example Requests (Postman Collection)

A Postman collection is available in the `api-responses/` directory (if included).

### OpenAPI/Swagger Documentation

**Future Enhancement:** OpenAPI 3.0 specifications will be auto-generated from code annotations.

**Planned Endpoints:**

- Product Service: `http://localhost:8081/swagger-ui.html`
- Order Service: `http://localhost:8083/swagger-ui.html`
- Inventory Service: `http://localhost:8082/swagger-ui.html`

---

## 🛑 Troubleshooting

### Common Issues and Solutions

#### 1. Services Not Starting

**Problem:** `docker-compose up` fails or services are unhealthy

**Solution:**

```bash
# Check logs for specific service
docker-compose logs <service-name>

# Common fixes:
# 1. Ensure ports are not in use
lsof -i :8080  # Check if port 8080 is in use
lsof -i :8761  # Check if port 8761 is in use

# 2. Rebuild images
mvn clean compile jib:dockerBuild
docker-compose up -d --force-recreate

# 3. Remove old containers and volumes
docker-compose down -v
docker system prune -a
```

---

#### 2. Unauthorized API Calls (401 Error)

**Problem:** API returns `401 Unauthorized`

**Solution:**

```bash
# 1. Verify Keycloak is running
curl http://localhost:8181/health

# 2. Get a fresh access token
curl -X POST 'http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=spring-cloud-client' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  -d 'grant_type=client_credentials'

# 3. Ensure realm and client are configured correctly in Keycloak
# See Step 6 in deployment guide
```

---

#### 3. Order Placement Fails (Insufficient Inventory)

**Problem:** Order fails with "Insufficient inventory" error

**Solution:**

```bash
# Check inventory database
docker exec -it mysql-inventory mysql -u inventoryuser -p
# Password: value from .env (default: password)

USE inventory_service;
SELECT * FROM t_inventory;

# If no inventory records exist, insert sample data:
INSERT INTO t_inventory (sku_code, quantity) VALUES ('SKU-001', 100);
INSERT INTO t_inventory (sku_code, quantity) VALUES ('SKU-002', 50);
```

---

#### 4. Service Not Registered in Eureka

**Problem:** Service doesn't appear in Eureka dashboard

**Solution:**

```bash
# 1. Check if service is running
docker-compose ps

# 2. Check service logs for Eureka registration errors
docker-compose logs <service-name> | grep -i eureka

# 3. Verify Eureka credentials in .env match docker-compose.yml
# 4. Wait 30-60 seconds for registration to propagate

# 5. Restart service
docker-compose restart <service-name>
```

---

#### 5. Kafka Consumer Not Receiving Events

**Problem:** Notifications not sent after order placement

**Solution:**

```bash
# 1. Check if Kafka broker is healthy
docker-compose logs broker

# 2. Check Notification Service logs
docker-compose logs notification-service

# 3. Verify Kafka topic exists
docker exec -it broker kafka-topics --list --bootstrap-server localhost:9092
# Should see "notificationTopic"

# 4. Check consumer group status
docker exec -it broker kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group notification

# 5. Restart notification service
docker-compose restart notification-service
```

---

#### 6. Database Connection Errors

**Problem:** Service logs show database connection failures

**Solution:**

```bash
# 1. Check if database is healthy
docker-compose ps | grep mysql

# 2. Verify database credentials in .env
cat .env | grep DB_PASSWORD

# 3. Wait for database to be ready (healthcheck)
docker-compose logs mysql-order | grep "ready for connections"

# 4. Test database connection
docker exec -it mysql-order mysql -u orderuser -p
# Enter password from .env

# 5. Restart service after database is ready
docker-compose restart order-service
```

---

#### 7. Out of Memory Errors

**Problem:** Containers crash with OOM errors

**Solution:**

```bash
# 1. Check Docker resource limits
docker stats

# 2. Increase Docker memory allocation:
# Docker Desktop: Settings → Resources → Memory (increase to 8GB+)

# 3. Reduce service memory limits in docker-compose.yml
# Edit deploy.resources.limits.memory values

# 4. Stop unnecessary services
docker-compose stop grafana prometheus
```

---

#### 8. Port Already in Use

**Problem:** `Error starting userland proxy: listen tcp 0.0.0.0:8080: bind: address already in use`

**Solution:**

```bash
# 1. Find process using the port
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows

# 2. Kill the process or change port in docker-compose.yml
# Example: Change API Gateway port
# ports:
#   - "8081:8080"  # External:Internal
```

---

#### 9. Zipkin Not Showing Traces

**Problem:** No traces appear in Zipkin UI

**Solution:**

```bash
# 1. Verify Zipkin is running
curl http://localhost:9411/health

# 2. Check if services are sending traces
docker-compose logs order-service | grep -i zipkin

# 3. Verify ZIPKIN_BASE_URL in .env
cat .env | grep ZIPKIN

# 4. Make an API call to generate traces
curl -X GET 'http://localhost:8080/api/product' \
  -H 'Authorization: Bearer <token>'

# 5. Refresh Zipkin UI and run query
```

---

### Getting Help

If issues persist:

1. **Check Logs:**
   ```bash
   docker-compose logs -f --tail=100
   ```

2. **Verify Environment:**
   ```bash
   docker --version
   docker-compose --version
   java -version
   mvn -version
   ```

3. **Review Documentation:**
    - [ARCHITECTURE.md](ARCHITECTURE.md)
    - [SERVICE_BOUNDARIES.md](SERVICE_BOUNDARIES.md)

4. **Open an Issue:**
    - GitHub Issues: https://github.com/PSesharao/stockxpress/issues

---

## 📄 Architecture Decision Records

StockXpress follows established microservices patterns. Key architectural decisions are documented below.

### ADR-001: Why Separate Services?

**Decision:** Decompose the system into separate microservices (Product, Order, Inventory, Notification)

**Rationale:**

| Service | Reason for Separation |
|---------|----------------------|
| **Product Service** | - Different data model (document-oriented MongoDB)<br/>- Different scalability needs (high read traffic)<br/>- Independent evolution (product attributes change frequently)<br/>- Technology choice (MongoDB for flexible schema) |
| **Order Service** | - Core business transaction (requires strong consistency)<br/>- Orchestration layer (saga coordinator)<br/>- Complex business logic (validation, compensation)<br/>- Event publishing (central event source) |
| **Inventory Service** | - High contention resource (concurrent stock checks)<br/>- Read-optimized (cacheable queries)<br/>- Future integration with WMS systems<br/>- Independent scaling for inventory checks |
| **Notification Service** | - Asynchronous processing (non-blocking)<br/>- External dependencies (email/SMS providers)<br/>- Eventual consistency acceptable<br/>- Multi-channel strategy (email, SMS, push) |

**Consequences:**

- ✅ **Pros:** Independent deployment, technology freedom, scalability, fault isolation
- ❌ **Cons:** Increased operational complexity, distributed transactions, network latency

**References:** [SERVICE_BOUNDARIES.md](SERVICE_BOUNDARIES.md)

---

### ADR-002: Database per Service Pattern

**Decision:** Each service owns its database (no shared databases)

**Databases:**

- Product Service → MongoDB (product catalog)
- Order Service → MySQL (orders, order line items)
- Inventory Service → MySQL (inventory records)
- Notification Service → MySQL (processed events)

**Rationale:**

- **Loose Coupling:** Schema changes don't affect other services
- **Technology Choice:** Use best database for each domain (MongoDB for flexible product attributes, MySQL for
  transactional orders)
- **Independent Scaling:** Scale databases independently based on load
- **Data Ownership:** Clear responsibility for data integrity

**Consequences:**

- ✅ **Pros:** Decoupling, technology freedom, independent scaling
- ❌ **Cons:** No ACID transactions across services, eventual consistency, data duplication

**Alternative Considered:** Shared database (rejected due to tight coupling)

---

### ADR-003: Synchronous vs Asynchronous Communication

**Decision:** Use synchronous HTTP for reads, asynchronous Kafka for writes

**Communication Patterns:**

| Pattern | Use Case | Technology |
|---------|----------|------------|
| **Synchronous HTTP** | Order → Inventory (stock check) | Spring WebClient + Circuit Breaker |
| **Asynchronous Kafka** | Order → Notification (order placed event) | Apache Kafka |

**Rationale:**

- **Synchronous for Critical Path:** Inventory check must succeed before order placement
- **Asynchronous for Non-Critical:** Notifications are eventual (can tolerate delay)
- **Resilience:** Circuit breaker prevents cascading failures on synchronous calls
- **Decoupling:** Notification Service doesn't need to know about Order Service

**Consequences:**

- ✅ **Pros:** Resilience, decoupling, scalability
- ❌ **Cons:** Complexity (two communication patterns), eventual consistency for notifications

---

### ADR-004: API Gateway Pattern

**Decision:** Use Spring Cloud Gateway as single entry point

**Rationale:**

- **Security:** Single point for JWT validation (OAuth 2.0)
- **Routing:** Client-agnostic routing to backend services
- **Load Balancing:** Distribute requests across service instances
- **Cross-Cutting Concerns:** CORS, rate limiting, request logging

**Consequences:**

- ✅ **Pros:** Simplified client integration, centralized security, load balancing
- ❌ **Cons:** Single point of failure (mitigated by clustering), additional network hop

---

### ADR-005: Service Discovery with Eureka

**Decision:** Use Netflix Eureka for dynamic service registration

**Rationale:**

- **Dynamic Scaling:** Services register/deregister automatically
- **Load Balancing:** Client-side load balancing across instances
- **Health Checks:** Automatic removal of unhealthy instances
- **Zero Configuration:** No manual service configuration in clients

**Alternatives Considered:**

- Consul (rejected: Eureka sufficient for current needs)
- Kubernetes DNS (future consideration for cloud deployment)

---

### ADR-006: Event-Driven Architecture with Kafka

**Decision:** Use Apache Kafka for asynchronous event streaming

**Rationale:**

- **Decoupling:** Publishers don't know about subscribers
- **Scalability:** Horizontal scaling of consumers
- **Durability:** Event persistence for replay
- **Ordering:** Guaranteed message ordering within partition

**Event Schema:**

- `OrderPlacedEvent` → Published by Order Service, consumed by Notification Service
- Idempotency via `ProcessedEvent` table (prevents duplicate notifications)

**Alternatives Considered:**

- RabbitMQ (rejected: Kafka better for event streaming and replay)
- AWS SQS (rejected: vendor lock-in)

---

### ADR-007: Observability with Zipkin

**Decision:** Use Zipkin for distributed tracing

**Rationale:**

- **Request Correlation:** Trace requests across multiple services
- **Latency Monitoring:** Identify performance bottlenecks
- **Error Tracing:** Diagnose failures in distributed systems
- **Spring Cloud Sleuth Integration:** Automatic trace ID propagation

**What We Track:**

- HTTP requests across services
- Kafka message publishing/consumption
- Database queries (future)

---

### ADR-008: OAuth 2.0 with Keycloak

**Decision:** Use Keycloak for OAuth 2.0 authentication

**Rationale:**

- **Industry Standard:** OAuth 2.0 is widely adopted
- **Stateless:** JWT tokens (no server-side sessions)
- **Centralized IAM:** Single identity provider for all services
- **Enterprise Ready:** Supports SSO, social login, LDAP integration

**Token Flow:**

1. Client requests token from Keycloak (client credentials grant)
2. Keycloak issues JWT token
3. Client sends token to API Gateway
4. Gateway validates token and forwards to backend service

**Alternatives Considered:**

- Spring Security Basic Auth (rejected: not suitable for production)
- Custom JWT implementation (rejected: Keycloak provides more features)

---

### ADR-009: Circuit Breaker Pattern

**Decision:** Use Resilience4j circuit breaker for external calls

**Rationale:**

- **Prevent Cascading Failures:** If Inventory Service is down, don't overwhelm it with requests
- **Fast Failure:** Return error immediately when circuit is open
- **Automatic Recovery:** Automatically retry when service recovers

**Configuration:**

- Failure threshold: 50%
- Wait duration: 5 seconds
- Permitted calls in half-open state: 3

**Applied To:**

- Order Service → Inventory Service calls

---

### ADR-010: API Versioning Strategy

**Decision:** Use URL-based versioning (`/api/v1/orders`, `/api/v2/orders`)

**Rationale:**

- **Clarity:** Version is immediately visible in URL
- **Routing:** Easy to route different versions to different implementations
- **Backward Compatibility:** Support multiple versions in parallel

**Versioning Policy:**

- Breaking changes → Major version increment (v1 → v2)
- Non-breaking changes → No version increment
- Deprecated versions supported for 2 release cycles (6 months)

**Current Status:** All APIs are v1 (implicit, no `/v1/` prefix yet)

**Future:** Add explicit versioning when introducing breaking changes

---

## 📁 Project Structure

```
stockxpress/
├── api-gateway/                 # API Gateway service
├── common-lib/                  # Shared library (DTOs, exceptions, enums)
├── discovery-server/            # Eureka service discovery
├── inventory-service/           # Inventory management service
├── notification-service/        # Notification service
├── order-service/               # Order processing service
├── product-service/             # Product catalog service
├── api-responses/               # Example API responses and screenshots
├── grafana/                     # Grafana dashboards and provisioning
├── prometheus/                  # Prometheus configuration
├── realms/                      # Keycloak realm configuration
├── docker-compose.yml           # Docker Compose orchestration
├── .env.example                 # Environment variables template
├── pom.xml                      # Maven parent POM
├── README.md                    # This file
├── ARCHITECTURE.md              # Detailed architecture documentation
├── DOMAIN_MODEL.md              # Domain model and DDD documentation
├── SERVICE_BOUNDARIES.md        # Service boundaries and contracts
└── project-architechture.png    # Architecture diagram
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork the Repository**
2. **Create a Feature Branch:** `git checkout -b feature/amazing-feature`
3. **Commit Changes:** `git commit -m 'Add amazing feature'`
4. **Push to Branch:** `git push origin feature/amazing-feature`
5. **Open Pull Request**

**Code Standards:**

- Follow existing code style
- Add unit tests for new features
- Update documentation

---

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 🙏 Acknowledgments

- This project concept draws inspiration from [Programming Techie](https://www.youtube.com/@ProgrammingTechie)
- Spring Boot and Spring Cloud communities
- Microservices patterns by Chris Richardson
- Domain-Driven Design by Eric Evans

---

## 📞 Contact

**Author:** Sesha Rao P  
**GitHub:** [@PSesharao](https://github.com/PSesharao)  
**Repository:** [stockxpress](https://github.com/PSesharao/stockxpress)

---

**Made with ❤️ using Spring Boot and Microservices**

