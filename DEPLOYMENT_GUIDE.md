# StockXpress Deployment Guide

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Deployment with Docker Compose](#local-deployment-with-docker-compose)
4. [Building Services Locally with Maven](#building-services-locally-with-maven)
5. [Environment Variable Configuration](#environment-variable-configuration)
6. [Database Initialization and Migrations](#database-initialization-and-migrations)
7. [Keycloak Realm Setup and User Creation](#keycloak-realm-setup-and-user-creation)
8. [Verification Steps](#verification-steps)
9. [Accessing Dashboards](#accessing-dashboards)
10. [Common Deployment Issues and Solutions](#common-deployment-issues-and-solutions)
11. [Scaling Services](#scaling-services)
12. [Backup and Restore Procedures](#backup-and-restore-procedures)
13. [Production Deployment Considerations](#production-deployment-considerations)

---

## Overview

This guide provides detailed instructions for deploying the StockXpress microservices platform in various environments. The deployment consists of:

### Microservices (6 services)
- **Discovery Server** (Eureka) - Port 8761
- **API Gateway** - Port 8080
- **Product Service** - Port 8081
- **Inventory Service** - Port 8082
- **Order Service** - Port 8083
- **Notification Service** - Port 8084

### Infrastructure Components
- **MongoDB** - Product data storage (Port 27017)
- **MySQL (Order)** - Order data storage (Port 3308)
- **MySQL (Inventory)** - Inventory data storage (Port 3307)
- **MySQL (Keycloak)** - Identity provider storage (Port 3306)
- **Redis** - Distributed caching (Port 6379)
- **Kafka + Zookeeper** - Event streaming (Ports 9092, 2181)
- **Keycloak** - OAuth 2.0 authentication (Port 8181)
- **Zipkin** - Distributed tracing (Port 9411)
- **Prometheus** - Metrics collection (Port 9090)
- **Grafana** - Metrics visualization (Port 3000)

---

## Prerequisites

### Required Software

| Software | Minimum Version | Recommended Version | Purpose |
|----------|----------------|---------------------|----------|
| Docker | 20.10.0 | 24.0.0+ | Container runtime |
| Docker Compose | 1.29.0 | 2.20.0+ | Multi-container orchestration |
| Java JDK | 8 | 11 | Building services |
| Apache Maven | 3.6.0 | 3.9.0+ | Build tool |
| Git | 2.30.0 | Latest | Source control |

### System Requirements

**Minimum:**
- CPU: 4 cores
- RAM: 8 GB
- Disk: 20 GB free space
- Network: Stable internet connection

**Recommended:**
- CPU: 8 cores
- RAM: 16 GB
- Disk: 50 GB SSD
- Network: High-speed connection

### Pre-Installation Checks

```bash
# Check Docker version
docker --version
# Expected: Docker version 20.10.0 or higher

# Check Docker Compose version
docker-compose --version
# Expected: Docker Compose version 1.29.0 or higher

# Check Java version
java -version
# Expected: openjdk version "1.8.0" or higher

# Check Maven version
mvn --version
# Expected: Apache Maven 3.6.0 or higher

# Check available memory
free -h  # Linux/Mac
wmic OS get FreePhysicalMemory  # Windows

# Check available disk space
df -h  # Linux/Mac
wmic logicaldisk get size,freespace,caption  # Windows
```

---

## Local Deployment with Docker Compose

### Step 1: Clone the Repository

```bash
# Clone the repository
git clone https://github.com/your-org/stockxpress.git
cd stockxpress

# Verify project structure
ls -la
# Should see: api-gateway, common-lib, discovery-server, inventory-service,
#             notification-service, order-service, product-service,
#             docker-compose.yml, pom.xml, etc.
```

### Step 2: Configure Environment Variables

```bash
# Copy the environment template
cp .env.example .env

# Edit the .env file with your configuration
# Use your preferred text editor (nano, vim, code, etc.)
nano .env
```

**Critical Variables to Configure:**

```bash
# Database Passwords (CHANGE THESE!)
MONGO_PASSWORD=your_secure_mongo_password
INVENTORY_DB_PASSWORD=your_secure_inventory_password
ORDER_DB_PASSWORD=your_secure_order_password
REDIS_PASSWORD=your_secure_redis_password

# Keycloak Admin Credentials (CHANGE THESE!)
KEYCLOAK_ADMIN_PASSWORD=your_secure_keycloak_admin_password
KEYCLOAK_MYSQL_PASSWORD=your_secure_keycloak_db_password

# Grafana Admin Credentials (CHANGE THESE!)
GRAFANA_ADMIN_PASSWORD=your_secure_grafana_password

# Eureka Credentials (CHANGE THESE!)
EUREKA_PASSWORD=your_secure_eureka_password
```

### Step 3: Build the Services

You have two options for building the services:

#### Option A: Build with Docker Compose (Recommended for Quick Start)

```bash
# Build all services using Docker Compose
docker-compose build

# This will:
# 1. Build common-lib as a shared dependency
# 2. Build all 6 microservices with multi-stage builds
# 3. Create optimized Docker images
# 4. Cache layers for faster subsequent builds
```

#### Option B: Build Locally with Maven First (for Development)

See [Building Services Locally with Maven](#building-services-locally-with-maven) section.

### Step 4: Start the Infrastructure

Start infrastructure components first to ensure databases and message brokers are ready:

```bash
# Start only infrastructure services
docker-compose up -d mongo mysql-order mysql-inventory mysql-keycloak \
  redis zookeeper broker keycloak zipkin prometheus grafana

# Wait for services to be healthy (about 2-3 minutes)
docker-compose ps

# Check logs if any service is unhealthy
docker-compose logs -f mongo
docker-compose logs -f mysql-order
```

### Step 5: Start the Microservices

Once infrastructure is healthy, start the microservices in order:

```bash
# Start discovery server first
docker-compose up -d discovery-server

# Wait 30 seconds for Eureka to be ready
sleep 30

# Check Eureka is up
curl -s http://localhost:8761/actuator/health | jq

# Start API Gateway
docker-compose up -d api-gateway
sleep 20

# Start business services
docker-compose up -d product-service inventory-service order-service notification-service

# Monitor startup
docker-compose logs -f product-service inventory-service order-service notification-service
```

### Step 6: Verify All Services Are Running

```bash
# Check all containers are up
docker-compose ps

# Expected output: All services with status "Up" and "healthy"

# Check service registration in Eureka
curl -s http://localhost:8761/eureka/apps | grep '<status>UP</status>' | wc -l
# Should return 5 (api-gateway, product, inventory, order, notification)
```

### Step 7: Quick Start - All Services at Once (Alternative)

If you prefer to start everything together:

```bash
# Start all services at once
docker-compose up -d

# Wait for all services to be healthy (5-7 minutes)
watch -n 5 'docker-compose ps'

# Once all show "healthy", press Ctrl+C to exit watch
```

### Step 8: View Logs

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f product-service

# View last 100 lines
docker-compose logs --tail=100 order-service

# Follow logs from multiple services
docker-compose logs -f api-gateway product-service inventory-service
```

### Step 9: Stop Services

```bash
# Stop all services
docker-compose stop

# Stop and remove containers (keeps volumes)
docker-compose down

# Stop and remove everything including volumes (CAUTION: deletes data)
docker-compose down -v
```

---

## Building Services Locally with Maven

### Full Build (All Services)

```bash
# From project root
cd stockxpress

# Clean and build all modules
mvn clean install -DskipTests

# This will:
# 1. Build common-lib first
# 2. Build all 6 microservices
# 3. Install JARs to local Maven repository
# 4. Skip tests for faster build
```

### Build Individual Services

```bash
# Build common-lib (required for all services)
cd common-lib
mvn clean install -DskipTests

# Build specific service
cd ../product-service
mvn clean package -DskipTests

# The JAR will be in target/ directory
ls -lh target/*.jar
```

### Running Tests During Build

```bash
# Run all tests
mvn clean install

# Run only unit tests
mvn clean test

# Run only integration tests
mvn clean verify -DskipUnitTests

# Run with coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Build Docker Images Locally

```bash
# Build Docker image with Jib (faster, no Docker daemon needed)
mvn clean compile jib:dockerBuild

# Build Docker image with Dockerfile
docker build -t stockxpress/product-service:latest -f product-service/Dockerfile .

# Build all services
for service in discovery-server api-gateway product-service inventory-service order-service notification-service; do
  docker build -t stockxpress/$service:latest -f $service/Dockerfile .
done
```

### Running Services Locally (Without Docker)

```bash
# Ensure infrastructure is running in Docker
docker-compose up -d mongo mysql-order mysql-inventory redis broker zipkin

# Run discovery server
cd discovery-server
mvn spring-boot:run

# In new terminal: Run product service
cd product-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# Repeat for other services in new terminals
```

---

## Environment Variable Configuration

### Using .env File (Recommended)

Docker Compose automatically loads variables from `.env` file:

```bash
# .env file structure
SPRING_PROFILES_ACTIVE=docker
MONGO_PASSWORD=secure_password_here
ORDER_DB_PASSWORD=another_secure_password
# ... other variables
```

### Using Environment-Specific Files

```bash
# Create environment-specific files
cp .env.example .env.dev
cp .env.example .env.staging
cp .env.example .env.prod

# Use specific env file
docker-compose --env-file .env.prod up -d
```

### Overriding Variables

```bash
# Override specific variable
MONGO_PASSWORD=newpassword docker-compose up -d mongo

# Override multiple variables
env MONGO_PASSWORD=pass1 ORDER_DB_PASSWORD=pass2 docker-compose up -d
```

### Required Variables by Service

#### Product Service
```bash
SPRING_DATA_MONGODB_HOST=mongo
SPRING_DATA_MONGODB_PORT=27017
SPRING_DATA_MONGODB_DATABASE=product-service
SPRING_DATA_MONGODB_USERNAME=root
SPRING_DATA_MONGODB_PASSWORD=${MONGO_PASSWORD}
```

#### Inventory Service
```bash
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-inventory:3306/inventory_service
SPRING_DATASOURCE_USERNAME=inventoryuser
SPRING_DATASOURCE_PASSWORD=${INVENTORY_DB_PASSWORD}
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=${REDIS_PASSWORD}
```

#### Order Service
```bash
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-order:3306/order_service
SPRING_DATASOURCE_USERNAME=orderuser
SPRING_DATASOURCE_PASSWORD=${ORDER_DB_PASSWORD}
KAFKA_BOOTSTRAP_SERVERS=broker:29092
```

#### Notification Service
```bash
KAFKA_BOOTSTRAP_SERVERS=broker:29092
SPRING_KAFKA_CONSUMER_GROUP_ID=notificationGroup
```

#### API Gateway
```bash
KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/spring-boot-microservices-realm
KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/spring-boot-microservices-realm/protocol/openid-connect/certs
```

---

## Database Initialization and Migrations

### Automatic Initialization

All databases are automatically initialized on first startup:

```bash
# MySQL databases create schemas automatically via JPA
# spring.jpa.hibernate.ddl-auto=update (in application.yml)

# MongoDB creates database on first connection

# Verify databases were created
docker exec -it mysql-order mysql -uroot -p${ORDER_DB_PASSWORD} -e "SHOW DATABASES;"
docker exec -it mysql-inventory mysql -uroot -p${INVENTORY_DB_PASSWORD} -e "SHOW DATABASES;"
docker exec -it mongo mongosh --eval "show dbs"
```

### Manual Schema Creation (Optional)

#### Order Service MySQL Schema

```bash
# Connect to order database
docker exec -it mysql-order mysql -uroot -p${ORDER_DB_PASSWORD}

# Create schema manually
USE order_service;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    sku_code VARCHAR(255) NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
```

#### Inventory Service MySQL Schema

```bash
# Connect to inventory database
docker exec -it mysql-inventory mysql -uroot -p${INVENTORY_DB_PASSWORD}

# Create schema manually
USE inventory_service;

CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_code VARCHAR(255) UNIQUE NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sku_code (sku_code),
    INDEX idx_quantity (quantity)
);
```

### Sample Data Loading

#### Load Product Data

```bash
# Connect to MongoDB
docker exec -it mongo mongosh

# Switch to product-service database
use product-service

# Insert sample products
db.product.insertMany([
  {
    name: "iPhone 15 Pro",
    description: "Latest Apple flagship smartphone",
    price: NumberDecimal("999.99")
  },
  {
    name: "Samsung Galaxy S24",
    description: "Premium Android smartphone",
    price: NumberDecimal("899.99")
  },
  {
    name: "MacBook Pro 16\"",
    description: "Professional laptop for developers",
    price: NumberDecimal("2499.99")
  }
]);

# Verify insertion
db.product.find().pretty();
```

#### Load Inventory Data

```bash
# Connect to inventory MySQL
docker exec -it mysql-inventory mysql -uroot -p${INVENTORY_DB_PASSWORD}

USE inventory_service;

# Insert sample inventory
INSERT INTO inventory (sku_code, quantity) VALUES
('IPHONE_15_PRO', 100),
('SAMSUNG_S24', 150),
('MACBOOK_PRO_16', 50),
('IPAD_AIR', 200),
('AIRPODS_PRO', 300);

# Verify insertion
SELECT * FROM inventory;
```

### Database Migrations with Flyway (Optional Enhancement)

For production, consider using Flyway for versioned migrations:

```xml
<!-- Add to service pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

Create migration files:
```bash
mkdir -p order-service/src/main/resources/db/migration

# Create V1__initial_schema.sql
cat > order-service/src/main/resources/db/migration/V1__initial_schema.sql <<EOF
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
EOF

# Create V2__add_order_line_items.sql for next version
```

---

## Keycloak Realm Setup and User Creation

### Step 1: Access Keycloak Admin Console

```bash
# Wait for Keycloak to be ready
docker-compose logs -f keycloak | grep "Keycloak.*started"

# Open Keycloak in browser
open http://localhost:8181

# Login credentials (from .env):
# Username: admin
# Password: ${KEYCLOAK_ADMIN_PASSWORD}
```

### Step 2: Import Realm Configuration

#### Option A: Import Pre-configured Realm (If Available)

```bash
# Check if realm file exists
ls -l realms/spring-boot-microservices-realm.json

# If exists, Keycloak auto-imports on startup
# Verify in admin console: Master dropdown → spring-boot-microservices-realm
```

#### Option B: Create Realm Manually

1. **Create Realm:**
   - Click **Master** dropdown → **Create Realm**
   - Name: `spring-boot-microservices-realm`
   - Click **Create**

2. **Create Client:**
   - Go to **Clients** → **Create Client**
   - Client ID: `spring-cloud-client`
   - Client Protocol: `openid-connect`
   - Click **Save**
   - Access Type: `confidential`
   - Valid Redirect URIs: `http://localhost:8080/*`
   - Click **Save**
   - Go to **Credentials** tab
   - Copy **Client Secret** (needed for API gateway)

3. **Create Roles:**
   - Go to **Realm Roles** → **Create Role**
   - Create roles: `user`, `admin`, `operator`

4. **Create Users:**
   - Go to **Users** → **Create User**
   - Username: `testuser`
   - Email: `testuser@stockxpress.com`
   - First Name: `Test`
   - Last Name: `User`
   - Email Verified: `ON`
   - Click **Create**
   
5. **Set User Password:**
   - Go to **Credentials** tab
   - Set Password: `password123`
   - Temporary: `OFF`
   - Click **Set Password**

6. **Assign Roles:**
   - Go to **Role Mappings** tab
   - Available Roles: Select `user`
   - Click **Assign**

### Step 3: Export Realm Configuration

```bash
# Export realm for version control
mkdir -p realms

# Export via Keycloak admin console:
# 1. Select realm
# 2. Go to Realm Settings → Action → Partial Export
# 3. Check "Export clients" and "Export roles"
# 4. Download JSON
# 5. Save to realms/spring-boot-microservices-realm.json

# Or export via CLI
docker exec keycloak /opt/keycloak/bin/kc.sh export \
  --dir /opt/keycloak/data/export \
  --realm spring-boot-microservices-realm

# Copy exported file
docker cp keycloak:/opt/keycloak/data/export/spring-boot-microservices-realm.json realms/
```

### Step 4: Get Access Token

```bash
# Request access token
curl -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" | jq

# Extract access token
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" | jq -r '.access_token')

echo "Access Token: $ACCESS_TOKEN"
```

### Step 5: Test Authentication

```bash
# Test authenticated API call
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/product

# Should return product list

# Test without token (should fail)
curl http://localhost:8080/api/product
# Should return 401 Unauthorized
```

---

## Verification Steps

### 1. Check Service Health

```bash
# Check all container statuses
docker-compose ps

# Check health endpoints for each service
for port in 8761 8080 8081 8082 8083 8084; do
  echo "Checking port $port:"
  curl -s http://localhost:$port/actuator/health | jq
done
```

### 2. Verify Service Registration

```bash
# Check Eureka dashboard
open http://localhost:8761

# API check for registered services
curl -s http://localhost:8761/eureka/apps | grep -o '<app>[^<]*</app>'

# Expected output:
# <app>GATEWAY</app>
# <app>PRODUCT-SERVICE</app>
# <app>INVENTORY-SERVICE</app>
# <app>ORDER-SERVICE</app>
# <app>NOTIFICATION-SERVICE</app>
```

### 3. Test API Endpoints

```bash
# Get access token first (see Keycloak section)
ACCESS_TOKEN="your_token_here"

# Test Product Service
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/product

# Test Inventory Service
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  "http://localhost:8080/api/inventory?skuCode=IPHONE_15_PRO&skuCode=SAMSUNG_S24"

# Test Order Service
curl -X POST -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderLineItemDtoList": [
      {
        "skuCode": "IPHONE_15_PRO",
        "price": 999.99,
        "quantity": 1
      }
    ]
  }' \
  http://localhost:8080/api/order
```

### 4. Check Database Connections

```bash
# MongoDB
docker exec -it mongo mongosh --eval "db.adminCommand('ping')"

# MySQL Order
docker exec -it mysql-order mysqladmin -uroot -p${ORDER_DB_PASSWORD} ping

# MySQL Inventory
docker exec -it mysql-inventory mysqladmin -uroot -p${INVENTORY_DB_PASSWORD} ping

# Redis
docker exec -it redis redis-cli -a ${REDIS_PASSWORD} ping
```

### 5. Verify Kafka Topics

```bash
# List Kafka topics
docker exec -it broker kafka-topics --bootstrap-server localhost:9092 --list

# Expected output should include:
# notificationTopic

# Check topic details
docker exec -it broker kafka-topics --bootstrap-server localhost:9092 \
  --describe --topic notificationTopic

# Consume messages (test notification flow)
docker exec -it broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic notificationTopic \
  --from-beginning
```

### 6. Check Distributed Tracing

```bash
# Open Zipkin UI
open http://localhost:9411

# Make some API calls to generate traces
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/product

# Refresh Zipkin UI and search for traces
# You should see traces showing the request flow:
# Gateway → Product Service → MongoDB
```

### 7. Verify Metrics Collection

```bash
# Check Prometheus targets
open http://localhost:9090/targets

# All microservices should show "UP" status

# Query a metric
curl 'http://localhost:9090/api/v1/query?query=up'

# Check service-specific metrics
curl 'http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count'
```

### 8. End-to-End Flow Test

```bash
#!/bin/bash
# Complete order flow test

set -e

echo "1. Getting access token..."
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" | jq -r '.access_token')

echo "2. Creating product..."
PRODUCT_ID=$(curl -s -X POST -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Product","description":"Test","price":99.99}' \
  http://localhost:8080/api/product | jq -r '.id')

echo "3. Checking inventory..."
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  "http://localhost:8080/api/inventory?skuCode=TEST_SKU"

echo "4. Placing order..."
ORDER_NUMBER=$(curl -s -X POST -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"orderLineItemDtoList":[{"skuCode":"IPHONE_15_PRO","price":999.99,"quantity":1}]}' \
  http://localhost:8080/api/order)

echo "5. Verifying order in database..."
docker exec -it mysql-order mysql -uroot -p${ORDER_DB_PASSWORD} \
  -e "SELECT * FROM order_service.orders ORDER BY id DESC LIMIT 1;"

echo "6. Checking notification was sent (Kafka)..."
docker exec -it broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic notificationTopic \
  --max-messages 1 \
  --timeout-ms 5000

echo "✅ End-to-end test completed successfully!"
```

---

## Accessing Dashboards

### Eureka Discovery Dashboard

```bash
# URL: http://localhost:8761
# No authentication required

open http://localhost:8761

# What to check:
# - All 5 services registered (Gateway, Product, Inventory, Order, Notification)
# - All services show status "UP"
# - Instance counts match expected replicas
```

### Zipkin Tracing Dashboard

```bash
# URL: http://localhost:9411
# No authentication required

open http://localhost:9411

# How to use:
# 1. Click "Find Traces"
# 2. Select service name (e.g., "gateway")
# 3. Click "Run Query"
# 4. Click on a trace to see detailed flow
# 5. Look for slow spans (highlighted in yellow/red)
```

### Prometheus Metrics Dashboard

```bash
# URL: http://localhost:9090
# No authentication required

open http://localhost:9090

# Useful queries:
# - Service uptime: up{job="product-service"}
# - Request rate: rate(http_server_requests_seconds_count[5m])
# - Error rate: rate(http_server_requests_seconds_count{status=~"5.."}[5m])
# - Latency p95: histogram_quantile(0.95, http_server_requests_seconds_bucket)
# - JVM memory: jvm_memory_used_bytes
```

### Grafana Visualization Dashboard

```bash
# URL: http://localhost:3000
# Default credentials: admin / admin (change on first login)

open http://localhost:3000

# Initial setup:
# 1. Login with admin/admin
# 2. Change password when prompted
# 3. Prometheus datasource should be auto-configured
# 4. Import dashboards from grafana/dashboards/

# Pre-built dashboards:
# - Microservices Overview: Health, latency, throughput, errors
# - Order Processing: Order flow metrics, inventory checks
# - Infrastructure: Database, Kafka, Redis metrics
# - JVM Metrics: Heap, GC, threads
```

### Keycloak Admin Console

```bash
# URL: http://localhost:8181
# Credentials: admin / ${KEYCLOAK_ADMIN_PASSWORD}

open http://localhost:8181

# Common tasks:
# - Manage users: Users → View all users
# - Manage roles: Realm Roles
# - View sessions: Sessions → Active sessions
# - Check events: Events → Login events
# - Client configuration: Clients → spring-cloud-client
```

### Kafka UI (Optional - Add to docker-compose.yml)

```yaml
# Add to docker-compose.yml for Kafka visualization
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  container_name: kafka-ui
  ports:
    - "8090:8080"
  environment:
    KAFKA_CLUSTERS_0_NAME: stockxpress
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: broker:29092
    KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
  depends_on:
    - broker
    - zookeeper
  networks:
    - stockxpress-network
```

Access at: http://localhost:8090

---

## Common Deployment Issues and Solutions

### Issue 1: Services Not Starting

**Symptoms:**
```bash
docker-compose ps
# Shows services as "Exit 1" or "Restarting"
```

**Diagnosis:**
```bash
# Check logs
docker-compose logs service-name

# Common errors:
# - "Connection refused" → Dependency not ready
# - "Port already in use" → Port conflict
# - "OutOfMemoryError" → Insufficient memory
```

**Solutions:**
```bash
# 1. Ensure dependencies are healthy first
docker-compose up -d mongo mysql-order mysql-inventory redis broker
sleep 60
docker-compose up -d discovery-server
sleep 30
docker-compose up -d api-gateway product-service inventory-service order-service

# 2. Check port conflicts
sudo lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows

# 3. Increase Docker memory (Docker Desktop → Settings → Resources)
# Minimum 8GB recommended

# 4. Restart Docker daemon
sudo systemctl restart docker  # Linux
# Or restart Docker Desktop
```

### Issue 2: Database Connection Errors

**Symptoms:**
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**Solutions:**
```bash
# 1. Verify database is running
docker-compose ps mysql-order

# 2. Check database health
docker exec mysql-order mysqladmin -uroot -p${ORDER_DB_PASSWORD} ping

# 3. Verify credentials in .env match docker-compose.yml
grep DB_PASSWORD .env

# 4. Check network connectivity
docker-compose exec order-service ping mysql-order

# 5. Restart database
docker-compose restart mysql-order

# 6. Check database logs
docker-compose logs mysql-order | grep ERROR
```

### Issue 3: Services Not Registering with Eureka

**Symptoms:**
```
Eureka dashboard shows no services or some services missing
```

**Solutions:**
```bash
# 1. Verify Eureka is accessible
curl http://localhost:8761/actuator/health

# 2. Check service logs for Eureka connection errors
docker-compose logs product-service | grep eureka

# 3. Verify Eureka URL configuration
docker-compose exec product-service env | grep EUREKA

# 4. Check network connectivity
docker-compose exec product-service ping discovery-server

# 5. Restart service
docker-compose restart product-service

# 6. Wait longer (initial registration can take 30-60 seconds)
sleep 60
curl http://localhost:8761/eureka/apps
```

### Issue 4: Keycloak Authentication Failures

**Symptoms:**
```
HTTP 401 Unauthorized when calling APIs
```

**Solutions:**
```bash
# 1. Verify Keycloak is running
curl http://localhost:8181/health/ready

# 2. Test token generation
curl -X POST http://localhost:8181/realms/spring-boot-microservices-realm/protocol/openid-connect/token \
  -d "username=testuser" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=spring-cloud-client" \
  -d "client_secret=YOUR_CLIENT_SECRET"

# 3. Verify realm exists
curl http://localhost:8181/realms/spring-boot-microservices-realm

# 4. Check API Gateway Keycloak configuration
docker-compose logs api-gateway | grep keycloak

# 5. Verify client secret matches in API Gateway config
# Check application.yml or environment variables

# 6. Ensure token is included in Authorization header
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/product
```

### Issue 5: Kafka Consumer Not Receiving Messages

**Symptoms:**
```
Notification service not processing order events
```

**Solutions:**
```bash
# 1. Verify Kafka is running
docker-compose ps broker

# 2. Check topics exist
docker exec broker kafka-topics --bootstrap-server localhost:9092 --list

# 3. Manually consume messages
docker exec broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic notificationTopic \
  --from-beginning

# 4. Check consumer group lag
docker exec broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group notificationGroup

# 5. Verify notification service is connected
docker-compose logs notification-service | grep kafka

# 6. Restart Kafka and consumers
docker-compose restart broker notification-service
```

### Issue 6: Out of Memory Errors

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**
```bash
# 1. Increase JVM heap size
# Add to docker-compose.yml environment:
JAVA_OPTS: "-Xms512m -Xmx1024m"

# 2. Reduce number of running services
docker-compose stop notification-service zipkin grafana

# 3. Increase Docker memory limit
# Docker Desktop → Settings → Resources → Memory: 8GB minimum

# 4. Check memory usage
docker stats

# 5. Clear unused Docker resources
docker system prune -a
```

### Issue 7: Slow Performance / High Latency

**Symptoms:**
```
API responses taking > 5 seconds
```

**Diagnosis:**
```bash
# 1. Check Zipkin for slow traces
open http://localhost:9411

# 2. Check database query performance
docker exec mysql-order mysql -uroot -p${ORDER_DB_PASSWORD} \
  -e "SHOW FULL PROCESSLIST;"

# 3. Check container resource usage
docker stats

# 4. Check Redis cache hit rate
docker exec redis redis-cli -a ${REDIS_PASSWORD} INFO stats
```

**Solutions:**
```bash
# 1. Enable caching
# Ensure Redis is running and configured

# 2. Add database indexes
# See Database Initialization section

# 3. Increase connection pool sizes
# In application.yml: spring.datasource.hikari.maximum-pool-size=20

# 4. Scale services
docker-compose up -d --scale product-service=3

# 5. Optimize queries
# Enable query logging and analyze slow queries
```

### Issue 8: Circuit Breaker Always Open

**Symptoms:**
```
Inventory check always failing with "Circuit breaker is OPEN"
```

**Solutions:**
```bash
# 1. Check inventory service health
curl http://localhost:8082/actuator/health

# 2. View circuit breaker state
curl http://localhost:8083/actuator/circuitbreakers

# 3. Check Resilience4j metrics
curl http://localhost:8083/actuator/metrics/resilience4j.circuitbreaker.state

# 4. Verify inventory service is registered
curl http://localhost:8761/eureka/apps/INVENTORY-SERVICE

# 5. Test direct inventory call
curl http://localhost:8082/api/inventory?skuCode=TEST

# 6. Restart order service to reset circuit breaker
docker-compose restart order-service

# 7. Adjust circuit breaker thresholds in application.yml
# resilience4j.circuitbreaker.configs.default.failure-rate-threshold=60
```

---

## Scaling Services

### Horizontal Scaling with Docker Compose

```bash
# Scale specific service to 3 instances
docker-compose up -d --scale product-service=3

# Scale multiple services
docker-compose up -d \
  --scale product-service=3 \
  --scale inventory-service=2 \
  --scale order-service=2

# View running instances
docker-compose ps

# Check load balancing in Eureka
open http://localhost:8761
# Should show multiple instances per service
```

### Scaling with Kubernetes

See [KUBERNETES_DEPLOYMENT.md](./KUBERNETES_DEPLOYMENT.md) for detailed Kubernetes deployment.

```bash
# Scale deployment
kubectl scale deployment product-service --replicas=5

# Autoscaling based on CPU
kubectl autoscale deployment product-service \
  --cpu-percent=70 \
  --min=2 \
  --max=10

# Check horizontal pod autoscaler
kubectl get hpa
```

### Monitoring Scaled Services

```bash
# Check all instances in Eureka
curl -s http://localhost:8761/eureka/apps/PRODUCT-SERVICE | grep '<status>'

# Monitor load distribution in Grafana
open http://localhost:3000
# Dashboard: Microservices Overview
# Check: Requests per instance

# Test load balancing
for i in {1..10}; do
  curl -H "Authorization: Bearer $ACCESS_TOKEN" \
    http://localhost:8080/api/product | grep -o '"hostAddress":"[^"]*"'
done
# Should see different IP addresses
```

---

## Backup and Restore Procedures

### Backup Databases

#### MongoDB Backup (Product Service)

```bash
# Create backup directory
mkdir -p backups/mongodb

# Backup all databases
docker exec mongo mongodump --out=/dump

# Copy backup to host
docker cp mongo:/dump backups/mongodb/dump-$(date +%Y%m%d-%H%M%S)

# Backup specific database
docker exec mongo mongodump --db=product-service --out=/dump/product-service
docker cp mongo:/dump/product-service backups/mongodb/

# Compressed backup
docker exec mongo mongodump --archive=/dump/product-service.archive --gzip
docker cp mongo:/dump/product-service.archive backups/mongodb/
```

#### MySQL Backup (Order & Inventory Services)

```bash
# Create backup directory
mkdir -p backups/mysql

# Backup order database
docker exec mysql-order mysqldump -uroot -p${ORDER_DB_PASSWORD} \
  --databases order_service \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  > backups/mysql/order-service-$(date +%Y%m%d-%H%M%S).sql

# Backup inventory database
docker exec mysql-inventory mysqldump -uroot -p${INVENTORY_DB_PASSWORD} \
  --databases inventory_service \
  --single-transaction \
  > backups/mysql/inventory-service-$(date +%Y%m%d-%H%M%S).sql

# Compressed backup
docker exec mysql-order mysqldump -uroot -p${ORDER_DB_PASSWORD} \
  --databases order_service | gzip > backups/mysql/order-service-$(date +%Y%m%d-%H%M%S).sql.gz
```

#### Redis Backup

```bash
# Redis auto-saves to dump.rdb
# Trigger manual save
docker exec redis redis-cli -a ${REDIS_PASSWORD} SAVE

# Copy RDB file
mkdir -p backups/redis
docker cp redis:/data/dump.rdb backups/redis/dump-$(date +%Y%m%d-%H%M%S).rdb
```

### Restore Databases

#### MongoDB Restore

```bash
# Copy backup to container
docker cp backups/mongodb/dump-20260908-120000 mongo:/dump

# Restore all databases
docker exec mongo mongorestore /dump

# Restore specific database
docker exec mongo mongorestore --db=product-service /dump/product-service

# Restore from archive
docker cp backups/mongodb/product-service.archive mongo:/dump/
docker exec mongo mongorestore --archive=/dump/product-service.archive --gzip
```

#### MySQL Restore

```bash
# Restore order database
docker exec -i mysql-order mysql -uroot -p${ORDER_DB_PASSWORD} \
  < backups/mysql/order-service-20260908-120000.sql

# Restore inventory database
docker exec -i mysql-inventory mysql -uroot -p${INVENTORY_DB_PASSWORD} \
  < backups/mysql/inventory-service-20260908-120000.sql

# Restore from compressed backup
gunzip < backups/mysql/order-service-20260908-120000.sql.gz | \
  docker exec -i mysql-order mysql -uroot -p${ORDER_DB_PASSWORD}
```

#### Redis Restore

```bash
# Stop Redis
docker-compose stop redis

# Copy backup to Redis data directory
docker cp backups/redis/dump-20260908-120000.rdb redis:/data/dump.rdb

# Start Redis
docker-compose start redis

# Verify data
docker exec redis redis-cli -a ${REDIS_PASSWORD} KEYS '*'
```

### Automated Backup Script

```bash
#!/bin/bash
# backup.sh - Automated backup script

set -e

BACKUP_DIR="backups"
DATE=$(date +%Y%m%d-%H%M%S)

echo "Starting backup at $DATE"

# MongoDB
echo "Backing up MongoDB..."
mkdir -p $BACKUP_DIR/mongodb
docker exec mongo mongodump --archive=/dump/full-$DATE.archive --gzip
docker cp mongo:/dump/full-$DATE.archive $BACKUP_DIR/mongodb/

# MySQL Order
echo "Backing up MySQL Order..."
mkdir -p $BACKUP_DIR/mysql
docker exec mysql-order mysqldump -uroot -p${ORDER_DB_PASSWORD} \
  --databases order_service | gzip > $BACKUP_DIR/mysql/order-$DATE.sql.gz

# MySQL Inventory
echo "Backing up MySQL Inventory..."
docker exec mysql-inventory mysqldump -uroot -p${INVENTORY_DB_PASSWORD} \
  --databases inventory_service | gzip > $BACKUP_DIR/mysql/inventory-$DATE.sql.gz

# Redis
echo "Backing up Redis..."
mkdir -p $BACKUP_DIR/redis
docker exec redis redis-cli -a ${REDIS_PASSWORD} SAVE
docker cp redis:/data/dump.rdb $BACKUP_DIR/redis/dump-$DATE.rdb

# Cleanup old backups (keep last 7 days)
find $BACKUP_DIR -type f -mtime +7 -delete

echo "Backup completed successfully at $(date)"
```

### Schedule Automated Backups

```bash
# Make script executable
chmod +x backup.sh

# Add to crontab (daily at 2 AM)
crontab -e

# Add this line:
0 2 * * * /path/to/stockxpress/backup.sh >> /var/log/stockxpress-backup.log 2>&1
```

---

## Production Deployment Considerations

### Security Hardening

1. **Use Secrets Management:**
```bash
# Use Docker secrets instead of environment variables
docker secret create mongo_password /path/to/password/file

# In docker-compose.yml:
secrets:
  mongo_password:
    external: true
services:
  mongo:
    secrets:
      - mongo_password
```

2. **Enable TLS/SSL:**
```yaml
# Add TLS certificates to services
volumes:
  - ./certs:/etc/ssl/certs:ro
environment:
  SPRING_PROFILES_ACTIVE: production,ssl
```

3. **Network Segmentation:**
```yaml
networks:
  frontend:
  backend:
  database:
services:
  api-gateway:
    networks:
      - frontend
      - backend
  product-service:
    networks:
      - backend
      - database
  mongo:
    networks:
      - database
```

### High Availability

1. **Database Replication:**
```yaml
# MySQL Master-Slave replication
mysql-master:
  image: mysql:8.0
  environment:
    MYSQL_REPLICATION_MODE: master

mysql-slave:
  image: mysql:8.0
  environment:
    MYSQL_REPLICATION_MODE: slave
    MYSQL_MASTER_HOST: mysql-master
```

2. **Service Redundancy:**
```bash
# Run multiple instances
docker-compose up -d --scale product-service=3 --scale order-service=3
```

3. **Load Balancing:**
```yaml
# Add HAProxy or Nginx for load balancing
load-balancer:
  image: haproxy:2.8
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./haproxy.cfg:/usr/local/etc/haproxy/haproxy.cfg:ro
```

### Monitoring and Alerting

1. **Alert Configuration:**
```yaml
# prometheus/alerts/critical.yml
groups:
  - name: critical
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 2m
        annotations:
          summary: "Service {{ $labels.job }} is down"
```

2. **Alertmanager Setup:**
```yaml
alertmanager:
  image: prom/alertmanager:latest
  ports:
    - "9093:9093"
  volumes:
    - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
```

### Resource Limits

```yaml
# Add resource limits to prevent resource exhaustion
services:
  product-service:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1024M
        reservations:
          cpus: '0.5'
          memory: 512M
```

### Health Checks

All services already include health checks. Monitor them:

```bash
# Check health status
docker-compose ps

# Detailed health check logs
docker inspect --format='{{json .State.Health}}' product-service | jq
```

---

## Summary

This deployment guide covers:

✅ **Complete deployment** from scratch to running system  
✅ **Multiple deployment methods** (Docker Compose, local Maven, Kubernetes)  
✅ **Security configuration** with Keycloak OAuth 2.0  
✅ **Database setup** with sample data  
✅ **Comprehensive verification** steps  
✅ **Dashboard access** for monitoring and tracing  
✅ **Troubleshooting** common issues  
✅ **Scaling strategies** for production  
✅ **Backup and restore** procedures  
✅ **Production best practices**  

For additional information:
- **Architecture:** See [ARCHITECTURE.md](./ARCHITECTURE.md)
- **Domain Model:** See [DOMAIN_MODEL.md](./DOMAIN_MODEL.md)
- **Service Boundaries:** See [SERVICE_BOUNDARIES.md](./SERVICE_BOUNDARIES.md)
- **Kubernetes:** See [KUBERNETES_DEPLOYMENT.md](./k8s/KUBERNETES_DEPLOYMENT.md)
- **CI/CD:** See [CICD.md](./CICD.md)

---

**Document Version:** 1.0  
**Last Updated:** September 8, 2026  
**Maintained By:** StockXpress DevOps Team