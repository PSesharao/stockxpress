# ElastiCache Redis Terraform Module

This module creates an AWS ElastiCache Redis cluster for caching in the StockXpress application, specifically designed for the Inventory Service and other microservices requiring fast data access.

## Features

- **High Performance**: In-memory Redis cache for sub-millisecond latency
- **High Availability**: 
  - Multi-AZ deployment with automatic failover
  - Read replicas for scaling read operations
  - Optional cluster mode for sharding
- **Security**:
  - TLS encryption in transit
  - Encryption at rest with KMS
  - Redis AUTH token authentication
  - VPC security groups
- **Monitoring**: CloudWatch logs, metrics, and alarms
- **Backup**: Automated daily snapshots with configurable retention
- **Scalability**: Vertical and horizontal scaling options

## Usage

### Basic Configuration (Non-Cluster Mode)

```hcl
module "elasticache" {
  source = "./modules/elasticache"
  
  project_name       = "stockxpress"
  environment        = "production"
  vpc_id             = module.vpc.vpc_id
  cache_subnet_ids   = module.vpc.private_subnet_ids
  
  node_type          = "cache.r5.large"
  num_cache_nodes    = 3  # 1 primary + 2 replicas
  engine_version     = "7.0"
  
  automatic_failover_enabled = true
  multi_az_enabled           = true
  
  tags = {
    Project     = "StockXpress"
    Environment = "production"
    ManagedBy   = "Terraform"
  }
}
```

### Cluster Mode Configuration (For Sharding)

```hcl
module "elasticache" {
  source = "./modules/elasticache"
  
  project_name       = "stockxpress"
  environment        = "production"
  vpc_id             = module.vpc.vpc_id
  cache_subnet_ids   = module.vpc.private_subnet_ids
  
  node_type              = "cache.r5.large"
  engine_version         = "7.0"
  
  cluster_mode_enabled   = true
  num_node_groups        = 3  # 3 shards
  replicas_per_node_group = 2  # 2 replicas per shard
  
  automatic_failover_enabled = true
  multi_az_enabled           = true
  
  tags = local.common_tags
}
```

## Architecture

### Use Cases in StockXpress

1. **Inventory Service**:
   - Cache inventory levels
   - Cache product availability
   - Reduce database load for frequent queries

2. **Product Service**:
   - Cache product catalog data
   - Cache frequently accessed product details

3. **Session Management**:
   - Store user sessions
   - Manage shopping cart data

4. **Rate Limiting**:
   - API rate limiting counters
   - Distributed rate limiting across services

### Replication Architecture

**Non-Cluster Mode**:
```
┌─────────────┐
│   Primary   │ ← Writes
└─────────────┘
       │
       ├─────────────┬─────────────┐
       ▼             ▼             ▼
  ┌─────────┐  ┌─────────┐  ┌─────────┐
  │ Replica │  │ Replica │  │ Replica │ ← Reads
  │   AZ-a  │  │   AZ-b  │  │   AZ-c  │
  └─────────┘  └─────────┘  └─────────┘
```

**Cluster Mode**:
```
Shard 1          Shard 2          Shard 3
┌────────┐      ┌────────┐      ┌────────┐
│Primary │      │Primary │      │Primary │
└────────┘      └────────┘      └────────┘
    │               │               │
    ▼               ▼               ▼
┌────────┐      ┌────────┐      ┌────────┐
│Replica │      │Replica │      │Replica │
└────────┘      └────────┘      └────────┘
```

### Security Configuration

1. **Network Security**:
   - Deployed in private subnets
   - Security group allows access only from VPC CIDR
   - No public access

2. **Encryption**:
   - TLS 1.2+ for in-transit encryption
   - AES-256 for at-rest encryption
   - Encrypted snapshots

3. **Authentication**:
   - Redis AUTH token (32 characters)
   - Token stored in AWS Secrets Manager
   - Token rotation support

## Spring Boot Integration

### Dependencies (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
</dependency>
```

### Configuration (application.yml)

```yaml
spring:
  redis:
    host: ${REDIS_ENDPOINT}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_AUTH_TOKEN}
    ssl: true
    timeout: 2000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 2
        max-wait: -1ms
      shutdown-timeout: 100ms
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour in milliseconds
      cache-null-values: false
```

### Java Configuration

```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Value("${spring.redis.host}")
    private String redisHost;
    
    @Value("${spring.redis.port}")
    private int redisPort;
    
    @Value("${spring.redis.password}")
    private String redisPassword;
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(redisPassword);
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .useSsl()
            .commandTimeout(Duration.ofSeconds(2))
            .build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(config)
            .build();
    }
}
```

### Usage Example

```java
@Service
public class InventoryService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "inventory", key = "#productId")
    public InventoryResponse getInventory(String productId) {
        // This will be cached
        return inventoryRepository.findByProductId(productId);
    }
    
    @CacheEvict(value = "inventory", key = "#productId")
    public void updateInventory(String productId, int quantity) {
        // This will invalidate the cache
        inventoryRepository.update(productId, quantity);
    }
    
    // Manual cache operations
    public void setWithExpiry(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }
}
```

## Monitoring

### CloudWatch Metrics

- **CPUUtilization**: CPU usage percentage
- **DatabaseMemoryUsagePercentage**: Memory usage
- **CurrConnections**: Current client connections
- **Evictions**: Number of evicted keys
- **CacheHits/CacheMisses**: Cache hit ratio
- **ReplicationLag**: Lag between primary and replicas
- **NetworkBytesIn/Out**: Network throughput

### CloudWatch Logs

- **Slow Log**: Commands taking longer than threshold
- **Engine Log**: Redis server logs

### Alarms

- High CPU utilization (>75%)
- High memory usage (>80%)
- High evictions (>100 in 5 minutes)
- High connection count

## Best Practices

1. **Connection Pooling**: Use connection pools (Lettuce has built-in pooling)
2. **TTL Strategy**: Set appropriate TTLs to prevent memory issues
3. **Key Naming**: Use consistent key naming convention (e.g., `service:entity:id`)
4. **Eviction Policy**: Use `allkeys-lru` for cache use case
5. **Monitoring**: Monitor cache hit ratio and evictions
6. **Failover Testing**: Regularly test automatic failover
7. **Cluster Mode**: Use cluster mode for datasets >100GB
8. **Reserved Memory**: Configure `reserved-memory-percent` for large datasets

## Cache Patterns

### Cache-Aside (Lazy Loading)

```java
public Product getProduct(String id) {
    // Try cache first
    Product product = (Product) redisTemplate.opsForValue().get("product:" + id);
    if (product != null) {
        return product;
    }
    
    // Cache miss - load from database
    product = productRepository.findById(id);
    
    // Update cache
    redisTemplate.opsForValue().set("product:" + id, product, 1, TimeUnit.HOURS);
    
    return product;
}
```

### Write-Through

```java
public void updateProduct(Product product) {
    // Update database
    productRepository.save(product);
    
    // Update cache
    redisTemplate.opsForValue().set("product:" + product.getId(), product, 1, TimeUnit.HOURS);
}
```

## Troubleshooting

### Connection Issues

```bash
# Test connectivity
telnet <endpoint> 6379

# Connect with redis-cli (with TLS)
redis-cli -h <endpoint> -p 6379 --tls --cacert /path/to/certificate.pem

# Authenticate
AUTH <auth-token>

# Test connection
PING
```

### Performance Issues

1. Check CloudWatch metrics for CPU, memory, and evictions
2. Review slow log for expensive commands
3. Analyze cache hit ratio
4. Consider scaling up node type or enabling cluster mode
5. Review key expiration strategy

### Common Commands

```bash
# Get cluster info
INFO replication
INFO stats

# Check memory usage
INFO memory

# Monitor commands in real-time
MONITOR

# Get slow log
SLOWLOG GET 10
```

## Cost Optimization

- **Development**: Single cache.t3.medium node
- **Staging**: 2 cache.t3.medium nodes (1 primary + 1 replica)
- **Production**: 3+ cache.r5.large nodes across AZs
- **Reserved Instances**: Use reserved nodes for production for 30-40% savings

## Migration and Scaling

### Vertical Scaling (Resize Node Type)

```bash
# Update node_type variable
terraform apply
```

### Horizontal Scaling (Add Replicas)

```bash
# Increase num_cache_nodes
terraform apply
```

### Enable Cluster Mode (Requires Recreation)

1. Create snapshot of existing cluster
2. Set `cluster_mode_enabled = true`
3. Apply changes (will recreate cluster)
4. Restore data from snapshot

## References

- [AWS ElastiCache Documentation](https://docs.aws.amazon.com/elasticache/)
- [Redis Best Practices](https://docs.aws.amazon.com/elasticache/latest/red-ug/BestPractices.html)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Lettuce Documentation](https://lettuce.io/)
