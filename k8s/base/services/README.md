# StockXpress Microservices - Kubernetes Manifests

This directory contains Kubernetes Deployment and Service manifests for the core StockXpress microservices.

## 📁 Contents

### Service Manifests

| Service | Deployment | Service | Port |
|---------|-----------|---------|------|
| Discovery Server (Eureka) | `discovery-server-deployment.yml` | `discovery-server-service.yml` | 8761 |
| API Gateway | `api-gateway-deployment.yml` | `api-gateway-service.yml` | 8080 |
| Product Service | `product-service-deployment.yml` | `product-service-service.yml` | 8081 |

### Configuration Manifests

- **`service-configmaps.yml`** - ConfigMaps for Eureka, MongoDB, Keycloak, and Zipkin configuration
- **`service-secrets.yml.template`** - Template for Secrets (Eureka, MongoDB, Keycloak credentials)

## 🏗️ Architecture Overview

```
┌─────────────────┐
│   API Gateway   │ (Port 8080)
│   (2 replicas)  │
└────────┬────────┘
         │
         ├──────────────┬──────────────┐
         │              │              │
┌────────▼────────┐ ┌──▼──────────┐ ┌─▼──────────┐
│ Product Service │ │Order Service│ │Inventory   │
│  (2 replicas)   │ │             │ │Service     │
└────────┬────────┘ └─────────────┘ └────────────┘
         │
         ├──────────────┐
         │              │
┌────────▼────────┐ ┌──▼──────────────┐
│    MongoDB      │ │ Discovery Server│
│                 │ │   (1 replica)   │
└─────────────────┘ └─────────────────┘
```

## 🚀 Deployment Order

**Important:** Services must be deployed in the following order to ensure proper dependency resolution:

### 1. Deploy Configuration Resources

```bash
# Apply ConfigMaps
kubectl apply -f service-configmaps.yml

# Create Secrets from template (after updating with actual credentials)
cp service-secrets.yml.template service-secrets.yml
# Edit service-secrets.yml with actual base64-encoded credentials
kubectl apply -f service-secrets.yml
```

### 2. Deploy Infrastructure Dependencies

Ensure the following infrastructure components are running:

```bash
# MongoDB
kubectl apply -f ../infrastructure/mongodb-statefulset.yml

# Zipkin (optional, for distributed tracing)
kubectl apply -f ../infrastructure/zipkin-deployment.yml

# Keycloak (optional, for OAuth2 authentication)
# kubectl apply -f ../infrastructure/keycloak-deployment.yml
```

### 3. Deploy Discovery Server (Eureka)

```bash
kubectl apply -f discovery-server-deployment.yml
kubectl apply -f discovery-server-service.yml

# Wait for Discovery Server to be ready
kubectl wait --for=condition=ready pod -l app=discovery-server --timeout=120s
```

### 4. Deploy Microservices

```bash
# Product Service
kubectl apply -f product-service-deployment.yml
kubectl apply -f product-service-service.yml

# Wait for services to register with Eureka
kubectl wait --for=condition=ready pod -l app=product-service --timeout=120s
```

### 5. Deploy API Gateway

```bash
kubectl apply -f api-gateway-deployment.yml
kubectl apply -f api-gateway-service.yml

# Wait for API Gateway to be ready
kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=120s
```

## 📋 Deployment Specifications

### Discovery Server (Eureka)

**Image:** `stockxpress/discovery-server:latest`

**Resources:**
- Requests: CPU 500m, Memory 512Mi
- Limits: CPU 1000m, Memory 1Gi

**Replicas:** 1 (singleton)

**Health Checks:**
- Liveness: `/actuator/health/liveness` (60s initial delay)
- Readiness: `/actuator/health/readiness` (30s initial delay)
- Startup: `/actuator/health` (30 failure attempts)

**Environment Variables:**
- `EUREKA_USERNAME` - From Secret `eureka-credentials`
- `EUREKA_PASSWORD` - From Secret `eureka-credentials`
- `SPRING_ZIPKIN_BASE_URL` - From ConfigMap `tracing-config`

### API Gateway

**Image:** `stockxpress/api-gateway:latest`

**Resources:**
- Requests: CPU 500m, Memory 512Mi
- Limits: CPU 1000m, Memory 1Gi

**Replicas:** 2

**Health Checks:**
- Liveness: `/actuator/health/liveness` (60s initial delay)
- Readiness: `/actuator/health/readiness` (40s initial delay)
- Startup: `/actuator/health` (30 failure attempts)

**Dependencies:**
- Init Container: Waits for `discovery-server:8761`

**Routes Configured:**
- `/api/product` → `lb://product-service`
- `/api/order` → `lb://order-service`
- `/api/inventory` → `lb://inventory-service`
- `/eureka/web` → `http://discovery-server:8761`
- `/eureka/**` → `http://discovery-server:8761` (static resources)

### Product Service

**Image:** `stockxpress/product-service:latest`

**Resources:**
- Requests: CPU 500m, Memory 512Mi
- Limits: CPU 1000m, Memory 1Gi

**Replicas:** 2

**Health Checks:**
- Liveness: `/actuator/health/liveness` (60s initial delay)
- Readiness: `/actuator/health/readiness` (40s initial delay)
- Startup: `/actuator/health` (30 failure attempts)

**Dependencies:**
- Init Containers:
  - Waits for `mongodb:27017`
  - Waits for `discovery-server:8761`

**Environment Variables:**
- MongoDB connection from ConfigMap `mongodb-config`
- MongoDB credentials from Secret `mongodb-credentials`
- Eureka URL from ConfigMap `eureka-config`
- Zipkin URL from ConfigMap `tracing-config`

**Features:**
- Caffeine cache enabled (1000 items, 30min TTL)
- Prometheus metrics exposed
- Distributed tracing with Zipkin

## 🔧 Configuration

### ConfigMaps

#### eureka-config
```yaml
eureka.url: "http://eureka:password@discovery-server:8761/eureka"
eureka.hostname: "discovery-server"
eureka.port: "8761"
```

#### mongodb-config
```yaml
mongodb.host: "mongodb"
mongodb.port: "27017"
mongodb.database.product: "product-service"
```

#### keycloak-config
```yaml
issuer.uri: "http://keycloak:8080/realms/stockxpress"
realm: "stockxpress"
```

#### tracing-config
```yaml
zipkin.url: "http://zipkin:9411"
sleuth.probability: "1.0"
```

### Secrets

All secrets use base64 encoding. To encode values:

```bash
echo -n 'your-value' | base64
```

#### eureka-credentials
- `username`: Eureka admin username
- `password`: Eureka admin password

#### mongodb-credentials
- `username`: MongoDB username
- `password`: MongoDB password

## 🔍 Verification

### Check Deployment Status

```bash
# All services
kubectl get deployments -l tier=infrastructure
kubectl get deployments -l tier=gateway
kubectl get deployments -l tier=backend

# Services
kubectl get services -l app=discovery-server
kubectl get services -l app=api-gateway
kubectl get services -l app=product-service

# Pods
kubectl get pods -l app=discovery-server
kubectl get pods -l app=api-gateway
kubectl get pods -l app=product-service
```

### Check Service Registration

```bash
# Port-forward to Eureka Dashboard
kubectl port-forward svc/discovery-server 8761:8761

# Access Eureka Dashboard
open http://localhost:8761
```

### Check Logs

```bash
# Discovery Server
kubectl logs -l app=discovery-server --tail=100 -f

# API Gateway
kubectl logs -l app=api-gateway --tail=100 -f

# Product Service
kubectl logs -l app=product-service --tail=100 -f
```

### Test API Gateway

```bash
# Port-forward to API Gateway
kubectl port-forward svc/api-gateway 8080:8080

# Test product endpoint
curl http://localhost:8080/api/product
```

## 📊 Monitoring & Observability

### Prometheus Metrics

All services expose Prometheus metrics at `/actuator/prometheus`:

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "<service-port>"
  prometheus.io/path: "/actuator/prometheus"
```

### Health Checks

Spring Boot Actuator endpoints:
- `/actuator/health` - Overall health
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics

### Distributed Tracing

All services send traces to Zipkin:

```bash
# Port-forward to Zipkin UI
kubectl port-forward svc/zipkin 9411:9411

# Access Zipkin UI
open http://localhost:9411
```

## 🔄 Rolling Updates

### Update Service Image

```bash
# Update image tag
kubectl set image deployment/product-service \
  product-service=stockxpress/product-service:v1.2.0

# Watch rollout status
kubectl rollout status deployment/product-service
```

### Rollback Deployment

```bash
# Rollback to previous version
kubectl rollout undo deployment/product-service

# Rollback to specific revision
kubectl rollout undo deployment/product-service --to-revision=2
```

## 🔐 Security Best Practices

### Production Secrets Management

**Do not use the template secrets in production!**

For production environments, use one of:

1. **Sealed Secrets**
   ```bash
   kubeseal --format=yaml < service-secrets.yml > sealed-secrets.yml
   kubectl apply -f sealed-secrets.yml
   ```

2. **External Secrets Operator**
   ```bash
   kubectl apply -f ../infrastructure/external-secrets-operator.yml
   ```

3. **Cloud Provider Secret Management**
   - AWS Secrets Manager
   - Azure Key Vault
   - GCP Secret Manager

### Network Policies

Consider implementing network policies to restrict pod-to-pod communication:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: product-service-network-policy
spec:
  podSelector:
    matchLabels:
      app: product-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: api-gateway
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: mongodb
    - podSelector:
        matchLabels:
          app: discovery-server
```

## 📝 Labels & Selectors

### Standard Labels

All manifests use consistent labeling:

```yaml
labels:
  app: <service-name>           # Service identifier
  tier: <infrastructure|gateway|backend>  # Application tier
  component: <component-type>   # Component category
```

**Tiers:**
- `infrastructure` - Discovery server, config server
- `gateway` - API Gateway
- `backend` - Business logic services

**Components:**
- `service-discovery` - Eureka server
- `api-routing` - Spring Cloud Gateway
- `business-logic` - Domain services

## 🐛 Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name>

# Check events
kubectl get events --sort-by='.lastTimestamp'

# Check init container logs
kubectl logs <pod-name> -c wait-for-mongodb
kubectl logs <pod-name> -c wait-for-discovery
```

### Service Not Registering with Eureka

1. Check Eureka credentials in secret
2. Verify Eureka URL in configmap
3. Check network connectivity:
   ```bash
   kubectl exec -it <pod-name> -- nc -zv discovery-server 8761
   ```

### MongoDB Connection Issues

```bash
# Check MongoDB service
kubectl get svc mongodb

# Test connection from product-service pod
kubectl exec -it <product-service-pod> -- nc -zv mongodb 27017
```

### Memory/CPU Issues

```bash
# Check resource usage
kubectl top pods

# Adjust JVM heap sizes in deployment
env:
  - name: JAVA_OPTS
    value: "-Xms512m -Xmx1024m"
```

## 🔗 Related Documentation

- [Infrastructure Manifests](../infrastructure/README.md)
- [Secret Management Guide](../infrastructure/SECRET_MANAGEMENT.md)
- [Deployment Guide](../../../DEPLOYMENT_GUIDE.md)
- [Architecture Overview](../../../ARCHITECTURE.md)

## 📞 Support

For issues or questions:
1. Check pod logs: `kubectl logs <pod-name>`
2. Check pod events: `kubectl describe pod <pod-name>`
3. Review application.properties for configuration
4. Check Eureka dashboard for service registration

## 📄 License

Part of the StockXpress microservices platform.