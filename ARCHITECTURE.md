# StockXpress Architecture Documentation

## Table of Contents
1. [C4 Model Overview](#c4-model-overview)
2. [Context Diagram (Level 1)](#context-diagram-level-1)
3. [Container Diagram (Level 2)](#container-diagram-level-2)
4. [Component Diagrams (Level 3)](#component-diagrams-level-3)
   - [API Gateway Components](#api-gateway-components)
   - [Order Service Components](#order-service-components)
5. [Technology Stack](#technology-stack)
6. [Infrastructure Components](#infrastructure-components)
7. [Communication Patterns](#communication-patterns)

---

## C4 Model Overview

The C4 model provides a structured approach to visualizing software architecture at different levels of abstraction:
- **Level 1 (Context)**: System context showing external actors and system boundaries
- **Level 2 (Container)**: Runtime containers (applications, databases, message brokers)
- **Level 3 (Component)**: Internal components within selected containers
- **Level 4 (Code)**: Implementation details (documented in source code)

---

## Context Diagram (Level 1)

### System in Environment

The Context diagram shows how StockXpress fits into the broader business environment and who uses it.

```mermaid
graph TB
    subgraph External Actors
        Customer[👤 Customer<br/>E-commerce User]
        Admin[👤 Administrator<br/>Platform Operator]
        External[🌐 External Systems<br/>Payment, Shipping]
    end

    subgraph StockXpress System
        System[🏢 StockXpress<br/>E-commerce Platform<br/><br/>Manages product catalog,<br/>order processing, inventory,<br/>and notifications]
    end

    subgraph External Services
        IAM[🔐 Keycloak<br/>Identity & Access Management]
        Monitor[📊 Zipkin<br/>Distributed Tracing]
    end

    Customer -->|Browse catalog,<br/>Place orders| System
    Admin -->|Manage products,<br/>View orders| System
    System -->|Integrate with| External
    System -->|Authenticate & Authorize| IAM
    System -->|Send traces| Monitor

    style System fill:#4A90E2,color:#fff
    style IAM fill:#E94B3C,color:#fff
    style Monitor fill:#F39C12,color:#fff
```

### Key Actors

| Actor | Description | Key Interactions |
|-------|-------------|------------------|
| **Customer** | End users of the e-commerce platform | Browse products, place orders, check order status |
| **Administrator** | Platform operators and business users | Manage product catalog, view inventory, monitor orders |
| **External Systems** | Third-party integrations | Payment processing, shipping, email delivery |
| **Keycloak** | IAM provider | OAuth 2.0 authentication, JWT token issuance |
| **Zipkin** | Observability platform | Distributed tracing, latency monitoring |

---

## Container Diagram (Level 2)

### All Services, Databases, and Message Brokers

```mermaid
graph TB
    subgraph Client Layer
        WebApp[🌐 Web Application<br/>React/Angular]
        Mobile[📱 Mobile App<br/>iOS/Android]
        API[🔧 API Consumer<br/>Third-party]
    end

    subgraph API Layer
        Gateway[🚪 API Gateway<br/>Spring Cloud Gateway<br/>Port: 8080<br/><br/>Routing, Load Balancing,<br/>Security]
    end

    subgraph Service Discovery
        Eureka[🔍 Discovery Server<br/>Netflix Eureka<br/>Port: 8761<br/><br/>Service Registration<br/>& Lookup]
    end

    subgraph Business Services
        ProductSvc[📦 Product Service<br/>Spring Boot<br/>Port: Dynamic<br/><br/>Product catalog management]
        OrderSvc[📝 Order Service<br/>Spring Boot<br/>Port: Dynamic<br/><br/>Order processing & orchestration]
        InventorySvc[📊 Inventory Service<br/>Spring Boot<br/>Port: Dynamic<br/><br/>Stock availability checks]
        NotificationSvc[📧 Notification Service<br/>Spring Boot<br/>Port: Dynamic<br/><br/>Event-driven notifications]
    end

    subgraph Data Layer
        MongoDB[(🍃 MongoDB<br/>Product Catalog<br/>Port: 27017)]
        OrderDB[(🐬 MySQL<br/>Order Database<br/>Port: 3306)]
        InventoryDB[(🐬 MySQL<br/>Inventory Database<br/>Port: 3307)]
        NotificationDB[(🐬 MySQL<br/>Notification Events<br/>Port: 3306)]
    end

    subgraph Message Broker
        Kafka[📨 Apache Kafka<br/>Port: 9092<br/><br/>Event streaming platform]
        Zookeeper[🐘 Zookeeper<br/>Port: 2181<br/><br/>Kafka coordination]
    end

    subgraph Infrastructure
        Keycloak[🔐 Keycloak<br/>Port: 8080<br/><br/>OAuth 2.0 & OpenID Connect]
        Zipkin[📊 Zipkin<br/>Port: 9411<br/><br/>Distributed tracing]
    end

    %% Client to Gateway
    WebApp -->|HTTPS/REST| Gateway
    Mobile -->|HTTPS/REST| Gateway
    API -->|HTTPS/REST| Gateway

    %% Gateway to Services
    Gateway -->|Load Balanced| ProductSvc
    Gateway -->|Load Balanced| OrderSvc
    Gateway -.->|Service Lookup| Eureka

    %% Service Registration
    ProductSvc -.->|Register| Eureka
    OrderSvc -.->|Register| Eureka
    InventorySvc -.->|Register| Eureka
    NotificationSvc -.->|Register| Eureka

    %% Service to Service Communication
    OrderSvc -->|Sync HTTP| InventorySvc
    OrderSvc -->|Async Kafka| Kafka
    Kafka -->|Subscribe| NotificationSvc

    %% Services to Databases
    ProductSvc -->|JDBC| MongoDB
    OrderSvc -->|JPA/Hibernate| OrderDB
    InventorySvc -->|JPA/Hibernate| InventoryDB
    NotificationSvc -->|JPA/Hibernate| NotificationDB

    %% Infrastructure
    Gateway -->|Validate JWT| Keycloak
    ProductSvc -->|Send Traces| Zipkin
    OrderSvc -->|Send Traces| Zipkin
    InventorySvc -->|Send Traces| Zipkin
    NotificationSvc -->|Send Traces| Zipkin
    Kafka -.->|Managed by| Zookeeper

    style Gateway fill:#4A90E2,color:#fff
    style ProductSvc fill:#2ECC71,color:#fff
    style OrderSvc fill:#2ECC71,color:#fff
    style InventorySvc fill:#2ECC71,color:#fff
    style NotificationSvc fill:#2ECC71,color:#fff
    style Eureka fill:#9B59B6,color:#fff
    style Kafka fill:#E67E22,color:#fff
    style Keycloak fill:#E74C3C,color:#fff
    style Zipkin fill:#F39C12,color:#fff
```

### Container Responsibilities

#### API Layer
| Container | Technology | Port | Responsibility |
|-----------|-----------|------|----------------|
| **API Gateway** | Spring Cloud Gateway | 8080 | Single entry point, routing, load balancing, JWT validation, CORS handling |

#### Service Discovery
| Container | Technology | Port | Responsibility |
|-----------|-----------|------|----------------|
| **Discovery Server** | Netflix Eureka | 8761 | Service registration, health checks, service discovery, load balancing metadata |

#### Business Services
| Container | Technology | Port | Responsibility |
|-----------|-----------|------|----------------|
| **Product Service** | Spring Boot 2.7.14 | Dynamic | Product CRUD operations, catalog management |
| **Order Service** | Spring Boot 2.7.14 | Dynamic | Order orchestration, inventory validation, saga coordination |
| **Inventory Service** | Spring Boot 2.7.14 | Dynamic | Stock availability checks, inventory queries |
| **Notification Service** | Spring Boot 2.7.14 | Dynamic | Event consumption, notification dispatch, idempotency |

#### Data Stores
| Container | Technology | Port | Data Stored |
|-----------|-----------|------|-------------|
| **MongoDB** | MongoDB 4.4.14 | 27017 | Product catalog (documents) |
| **Order Database** | MySQL 5.7 | 3306 | Orders, order line items |
| **Inventory Database** | MySQL 5.7 | 3307 | Inventory records, SKU quantities |
| **Notification Database** | MySQL 5.7 | 3306 | Processed events (idempotency) |

#### Message Broker
| Container | Technology | Port | Responsibility |
|-----------|-----------|------|----------------|
| **Apache Kafka** | Kafka 7.0.1 | 9092 | Event streaming, async messaging, topic: `notificationTopic` |
| **Zookeeper** | Zookeeper 7.0.1 | 2181 | Kafka cluster coordination, leader election |

#### Infrastructure
| Container | Technology | Port | Responsibility |
|-----------|-----------|------|----------------|
| **Keycloak** | Keycloak 18.0.0 | 8080 | OAuth 2.0 provider, JWT issuer, realm: `stockxpress` |
| **Zipkin** | Zipkin | 9411 | Distributed tracing, request correlation, latency analysis |

---

## Component Diagrams (Level 3)

### API Gateway Components

```mermaid
graph TB
    subgraph API Gateway Container
        GatewayApp[ApiGatewayApplication<br/>Main Entry Point]
        
        subgraph Routing Layer
            RouteConfig[Route Configuration<br/>/api/product → Product Service<br/>/api/order → Order Service<br/>/eureka/** → Discovery Server]
            LoadBalancer[Load Balancer<br/>Spring Cloud LoadBalancer]
        end
        
        subgraph Security Layer
            SecurityConfig[SecurityConfig<br/>OAuth 2.0 Resource Server]
            JwtDecoder[JWT Decoder<br/>Validates tokens from Keycloak]
            AuthFilter[Authorization Filter<br/>Enforces access control]
        end
        
        subgraph Cross-Cutting
            CorsFilter[CORS Filter<br/>Cross-origin requests]
            TracingFilter[Tracing Filter<br/>Spring Cloud Sleuth]
        end
    end
    
    Client[HTTP Client] -->|Request| GatewayApp
    GatewayApp --> CorsFilter
    CorsFilter --> AuthFilter
    AuthFilter --> JwtDecoder
    JwtDecoder -.->|Validate| Keycloak[Keycloak]
    AuthFilter --> RouteConfig
    RouteConfig --> LoadBalancer
    LoadBalancer -.->|Discover| Eureka[Eureka Server]
    LoadBalancer -->|Forward| Services[Business Services]
    GatewayApp --> TracingFilter
    TracingFilter -.->|Send Spans| Zipkin[Zipkin]
    
    style SecurityConfig fill:#E74C3C,color:#fff
    style RouteConfig fill:#3498DB,color:#fff
```

#### Key Components

| Component | Responsibility | Technology |
|-----------|----------------|------------|
| **ApiGatewayApplication** | Bootstrap application, route initialization | Spring Boot |
| **SecurityConfig** | Configure OAuth 2.0 resource server, JWT validation | Spring Security |
| **Route Configuration** | Define routing rules to downstream services | Spring Cloud Gateway |
| **Load Balancer** | Distribute requests across service instances | Spring Cloud LoadBalancer |
| **CORS Filter** | Handle cross-origin resource sharing | Spring Web |
| **Tracing Filter** | Correlation ID propagation, distributed tracing | Spring Cloud Sleuth |

---

### Order Service Components

```mermaid
graph TB
    subgraph Order Service Container
        OrderApp[OrderServiceApplication<br/>Main Entry Point]
        
        subgraph API Layer
            OrderController[OrderController<br/>REST API<br/>POST /api/order]
        end
        
        subgraph Business Logic Layer
            OrderService[OrderService<br/>Order orchestration<br/>Transaction management]
            OrderValidator[OrderValidator<br/>Business rule validation]
            OrderMapper[OrderMapper<br/>DTO ↔ Entity mapping]
        end
        
        subgraph External Integration
            InventoryClient[InventoryClient<br/>WebClient wrapper<br/>Circuit breaker enabled]
            OrderEventPublisher[OrderEventPublisher<br/>Kafka producer<br/>Topic: notificationTopic]
        end
        
        subgraph Data Access Layer
            OrderRepository[OrderRepository<br/>Spring Data JPA]
            OrderEntity[Order Entity<br/>OrderLineItem Entity]
        end
        
        subgraph Resilience Layer
            CircuitBreaker[Circuit Breaker<br/>Resilience4j<br/>Failure threshold: 50%]
            RetryPolicy[Retry Policy<br/>Max attempts: 3<br/>Exponential backoff]
            TimeLimiter[Time Limiter<br/>Timeout: 3s]
        end
        
        subgraph Cross-Cutting
            LoggingAspect[Logging Aspect<br/>@LogExecutionTime]
            CacheManager[Cache Manager<br/>Caffeine cache]
            ExceptionHandler[Global Exception Handler<br/>BusinessException handling]
        end
    end
    
    Client[API Gateway] -->|POST /api/order| OrderController
    OrderController --> OrderService
    OrderService --> OrderValidator
    OrderService --> OrderMapper
    OrderService --> InventoryClient
    InventoryClient --> CircuitBreaker
    CircuitBreaker --> RetryPolicy
    RetryPolicy --> TimeLimiter
    TimeLimiter -.->|HTTP Call| InventorySvc[Inventory Service]
    OrderService --> OrderRepository
    OrderRepository --> OrderEntity
    OrderEntity -.->|JPA| OrderDB[(Order Database)]
    OrderService --> OrderEventPublisher
    OrderEventPublisher -.->|Publish| Kafka[Kafka Broker]
    OrderService --> LoggingAspect
    LoggingAspect -.->|Traces| Zipkin[Zipkin]
    OrderController --> ExceptionHandler
    OrderService --> CacheManager
    
    style OrderService fill:#2ECC71,color:#fff
    style CircuitBreaker fill:#E67E22,color:#fff
    style OrderEventPublisher fill:#9B59B6,color:#fff
```

#### Key Components

| Component | Responsibility | Pattern/Technology |
|-----------|----------------|--------------------|
| **OrderController** | REST API endpoint, request/response handling | MVC Controller |
| **OrderService** | Orchestrate order placement workflow, saga coordination | Service Layer, Transaction Management |
| **OrderValidator** | Validate order requests, inventory availability | Validator Pattern |
| **OrderMapper** | Map between DTOs and entities | Mapper Pattern |
| **InventoryClient** | Synchronous HTTP calls to Inventory Service | Adapter Pattern, WebClient |
| **OrderEventPublisher** | Publish domain events to Kafka | Event Publisher Pattern |
| **OrderRepository** | Data persistence operations | Repository Pattern, Spring Data JPA |
| **Circuit Breaker** | Fault tolerance, fallback mechanisms | Circuit Breaker Pattern, Resilience4j |
| **Retry Policy** | Automatic retry with exponential backoff | Retry Pattern |
| **Time Limiter** | Timeout management for external calls | Timeout Pattern |
| **Logging Aspect** | Performance monitoring, parameter logging | AOP, Cross-Cutting Concern |
| **Cache Manager** | Cache inventory checks, order queries | Cache-Aside Pattern, Caffeine |
| **Exception Handler** | Centralized error handling | Exception Handler Pattern |

---

## Technology Stack

### Core Framework
- **Spring Boot**: 2.7.14
- **Spring Cloud**: 2021.0.8
- **Java**: 8
- **Maven**: Build tool

### Service Communication
| Technology | Purpose | Version |
|-----------|---------|----------|
| Spring Cloud Gateway | API Gateway | 2021.0.8 |
| Netflix Eureka | Service Discovery | 2021.0.8 |
| WebClient | HTTP Client (non-blocking) | Spring WebFlux |
| Apache Kafka | Event Streaming | 7.0.1 |

### Data Persistence
| Technology | Purpose | Version |
|-----------|---------|----------|
| Spring Data JPA | Relational data access | 2.7.14 |
| Spring Data MongoDB | Document data access | 2.7.14 |
| Hibernate | ORM | 5.6.x |
| MySQL | Relational database | 5.7 |
| MongoDB | Document database | 4.4.14 |

### Resilience & Fault Tolerance
| Technology | Purpose | Configuration |
|-----------|---------|---------------|
| Resilience4j Circuit Breaker | Prevent cascading failures | 50% failure threshold, 5s wait |
| Resilience4j Retry | Automatic retries | 3 attempts, exponential backoff |
| Resilience4j Time Limiter | Timeout management | 3s timeout |
| Caffeine Cache | In-memory caching | 10min TTL, 500 max entries |

### Security
| Technology | Purpose | Version |
|-----------|---------|----------|
| Keycloak | IAM, OAuth 2.0 provider | 18.0.0 |
| Spring Security | Security framework | 5.7.x |
| OAuth 2.0 | Authorization protocol | - |
| OpenID Connect | Authentication protocol | - |
| JWT | Token format | - |

### Observability
| Technology | Purpose | Version |
|-----------|---------|----------|
| Spring Cloud Sleuth | Distributed tracing | 2021.0.8 |
| Zipkin | Trace aggregation & visualization | Latest |
| Micrometer | Metrics facade | 1.9.x |
| Spring Boot Actuator | Health checks, metrics | 2.7.14 |

### Testing
| Technology | Purpose |
|-----------|----------|
| JUnit 5 | Unit testing |
| Mockito | Mocking framework |
| TestContainers | Integration testing | 1.18.3 |
| Spring Boot Test | Integration test support |

### DevOps
| Technology | Purpose | Version |
|-----------|---------|----------|
| Docker | Containerization | - |
| Docker Compose | Multi-container orchestration | v3 |
| Jib | Container image builder | 3.2.1 |

---

## Infrastructure Components

### Containerization
All services are containerized using Docker with Jib Maven plugin:
- **Base Image**: `eclipse-temurin:8-jre`
- **Registry**: Docker Hub (`seshrao/*`)
- **Build Tool**: Jib (no Docker daemon required)

### Service Discovery Flow
```mermaid
sequenceDiagram
    participant Service as Microservice
    participant Eureka as Eureka Server
    participant Gateway as API Gateway
    
    Service->>Eureka: Register (heartbeat every 5s)
    Eureka-->>Service: Registration confirmed
    Gateway->>Eureka: Fetch service registry (every 5s)
    Eureka-->>Gateway: Return service instances
    Note over Gateway: Cache service locations
    Gateway->>Service: Load-balanced request
    Service-->>Gateway: Response
```

### Health Check Configuration
- **Lease Renewal**: 5 seconds
- **Lease Expiration**: 10 seconds
- **Registry Fetch**: 5 seconds
- **IP Preference**: Enabled (use IP instead of hostname)

---

## Communication Patterns

### Synchronous Communication (HTTP/REST)

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Order as Order Service
    participant Inventory as Inventory Service
    
    Client->>Gateway: POST /api/order
    Gateway->>Gateway: Validate JWT
    Gateway->>Order: Forward request
    Order->>Order: Validate order
    Order->>Inventory: GET /api/inventory?skuCodes=...
    Inventory->>Inventory: Query database
    Inventory-->>Order: 200 OK [inventory status]
    Order->>Order: Check availability
    Order->>Order: Save order
    Order-->>Gateway: 201 Created
    Gateway-->>Client: 201 Created
```

**Use Cases:**
- Order Service → Inventory Service: Stock availability checks
- Client → Product Service: Product catalog queries
- Client → Order Service: Order placement

**Technologies:**
- Spring WebClient (reactive, non-blocking)
- Circuit Breaker for fault tolerance
- Retry with exponential backoff

### Asynchronous Communication (Kafka)

```mermaid
sequenceDiagram
    participant Order as Order Service
    participant Kafka as Kafka Broker
    participant Notification as Notification Service
    participant DB as Notification DB
    
    Order->>Order: Order saved successfully
    Order->>Kafka: Publish OrderPlacedEvent<br/>(topic: notificationTopic)
    Kafka-->>Order: Ack
    Note over Order: Continue processing
    Kafka->>Notification: Consume event
    Notification->>DB: Check if event processed<br/>(idempotency)
    alt Event not processed
        DB-->>Notification: Not found
        Notification->>Notification: Send email notification
        Notification->>DB: Save processed event
    else Event already processed
        DB-->>Notification: Already exists
        Notification->>Notification: Skip (deduplicate)
    end
```

**Use Cases:**
- Order Service → Notification Service: Order placed notifications
- Future: Inventory Service → Order Service: Stock level alerts

**Technologies:**
- Apache Kafka 7.0.1
- Topic: `notificationTopic`
- JSON serialization
- Idempotent consumer pattern

### Event Schema

**OrderPlacedEvent:**
```json
{
  "eventId": "uuid",
  "eventVersion": "1.0",
  "timestamp": "2024-01-15T10:30:00",
  "orderNumber": "ORD-123456",
  "orderId": 789,
  "orderLineItems": [
    {
      "skuCode": "SKU-001",
      "price": 29.99,
      "quantity": 2
    }
  ],
  "metadata": {
    "source": "order-service",
    "environment": "production"
  }
}
```

---

## Deployment Architecture

```mermaid
graph TB
    subgraph Docker Network
        subgraph Gateway Layer
            GW[API Gateway :8080]
        end
        
        subgraph Service Layer
            PS[Product Service<br/>Dynamic Port]
            OS[Order Service<br/>Dynamic Port]
            IS[Inventory Service<br/>Dynamic Port]
            NS[Notification Service<br/>Dynamic Port]
            DS[Discovery Server :8761]
        end
        
        subgraph Data Layer
            Mongo[(MongoDB :27017)]
            OrderDB[(MySQL :3306)]
            InvDB[(MySQL :3307)]
        end
        
        subgraph Messaging
            ZK[Zookeeper :2181]
            KF[Kafka :9092]
        end
        
        subgraph Infrastructure
            KC[Keycloak :8080]
            ZP[Zipkin :9411]
        end
    end
    
    GW --> PS
    GW --> OS
    OS --> IS
    OS --> KF
    KF --> NS
    PS --> Mongo
    OS --> OrderDB
    IS --> InvDB
    KF -.-> ZK
    GW -.-> KC
    GW -.-> DS
    PS -.-> DS
    OS -.-> DS
    IS -.-> DS
    NS -.-> DS
```

### Port Allocation
| Service | Port | Accessibility |
|---------|------|---------------|
| API Gateway | 8080 | Public |
| Discovery Server | 8761 | Internal (via Gateway) |
| Keycloak | 8181 | Public (realm endpoint) |
| Zipkin | 9411 | Internal |
| Kafka | 9092 | Internal |
| Zookeeper | 2181 | Internal |
| MongoDB | 27017 | Internal |
| MySQL (Order) | 3306 | Internal |
| MySQL (Inventory) | 3307 | Internal |
| Business Services | Dynamic | Internal (via Gateway) |

---

## Quality Attributes

### Scalability
- **Horizontal Scaling**: Each service can scale independently
- **Dynamic Port Assignment**: Prevents port conflicts
- **Load Balancing**: Client-side load balancing via Eureka
- **Stateless Services**: No session affinity required

### Reliability
- **Circuit Breaker**: Prevents cascading failures (50% threshold)
- **Retry Mechanism**: 3 attempts with exponential backoff
- **Timeout Management**: 3s timeout for external calls
- **Health Checks**: Continuous monitoring via Eureka
- **Event Deduplication**: Idempotent consumer pattern

### Observability
- **Distributed Tracing**: End-to-end request tracking with Zipkin
- **Correlation IDs**: MDC logging for request correlation
- **Metrics**: Actuator endpoints for Prometheus integration
- **Structured Logging**: JSON-formatted logs with context

### Security
- **OAuth 2.0**: Industry-standard authorization
- **JWT Tokens**: Stateless authentication
- **Centralized IAM**: Keycloak for user management
- **API Gateway**: Single security checkpoint

### Maintainability
- **Separation of Concerns**: Clear service boundaries
- **Common Library**: Shared utilities, exceptions, DTOs
- **Consistent Patterns**: Repository, Service, Controller layers
- **Comprehensive Testing**: Unit, integration, and contract tests

---

## Future Architecture Enhancements

1. **API Versioning**: URL-based versioning (`/api/v1/`, `/api/v2/`)
2. **GraphQL Gateway**: Alternative query language for flexible data fetching
3. **CQRS**: Separate read/write models for complex domains
4. **Saga Orchestrator**: Centralized saga coordination service
5. **Event Sourcing**: Store events instead of state for audit trail
6. **Service Mesh**: Istio for advanced traffic management
7. **Kubernetes**: Container orchestration for production
8. **API Rate Limiting**: Prevent abuse via Redis-backed throttling
9. **Feature Toggles**: Gradual feature rollout capability
10. **Multi-Region Deployment**: Geographic distribution for low latency

---

## References

- [C4 Model Documentation](https://c4model.com/)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Microservices Patterns](https://microservices.io/patterns/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Resilience4j Documentation](https://resilience4j.readme.io/)

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Maintained By**: StockXpress Architecture Team
