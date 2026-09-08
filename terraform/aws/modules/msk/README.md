# MSK (Managed Streaming for Kafka) Terraform Module

This module creates an AWS MSK (Managed Streaming for Apache Kafka) cluster for event-driven architecture in the StockXpress application.

## Features

- **Fully Managed Kafka**: AWS-managed Kafka with automated patching and upgrades
- **High Availability**: Multi-AZ deployment with automatic broker replacement
- **Security**:
  - TLS encryption in transit
  - Encryption at rest with KMS
  - SASL/SCRAM authentication
  - IAM authentication support
  - VPC security groups
- **Monitoring**: 
  - CloudWatch metrics and logs
  - Prometheus exporters (JMX, Node)
  - Custom CloudWatch alarms
- **Scalability**: Horizontal and vertical scaling support
- **Durability**: Configurable replication and retention

## Usage

### Basic Configuration

```hcl
module "msk" {
  source = "./modules/msk"
  
  project_name    = "stockxpress"
  environment     = "production"
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids
  
  kafka_version            = "3.5.1"
  broker_instance_type     = "kafka.m5.large"
  number_of_broker_nodes   = 3
  broker_volume_size       = 100
  
  enable_scram_authentication = true
  enable_iam_authentication   = true
  
  tags = {
    Project     = "StockXpress"
    Environment = "production"
    ManagedBy   = "Terraform"
  }
}
```

### Advanced Configuration with Custom Settings

```hcl
module "msk" {
  source = "./modules/msk"
  
  project_name    = "stockxpress"
  environment     = "production"
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids
  
  # Cluster settings
  kafka_version            = "3.5.1"
  broker_instance_type     = "kafka.m5.xlarge"
  number_of_broker_nodes   = 6
  broker_volume_size       = 500
  
  # Provisioned throughput
  enable_provisioned_throughput = true
  provisioned_throughput_mibps  = 250
  
  # Security
  enable_scram_authentication    = true
  enable_iam_authentication      = true
  enable_unauthenticated_access  = false
  client_broker_encryption       = "TLS"
  
  # Kafka configuration
  auto_create_topics_enable  = false
  default_replication_factor = 3
  min_insync_replicas        = 2
  num_partitions             = 6
  log_retention_hours        = 336  # 14 days
  compression_type           = "zstd"
  max_message_bytes          = 10485760  # 10 MB
  
  # Monitoring and logging
  enable_jmx_exporter       = true
  enable_node_exporter      = true
  enable_cloudwatch_logs    = true
  enable_s3_logs            = true
  log_retention_days        = 30
  
  tags = local.common_tags
}
```

## Architecture

### Event-Driven Architecture in StockXpress

```
┌─────────────────┐
│  Order Service  │──┐
└─────────────────┘  │
                     │  Publish Events
┌─────────────────┐  │  ┌──────────────┐
│Product Service  │──┼─▶│  MSK Cluster │
└─────────────────┘  │  │   (Kafka)    │
                     │  └──────────────┘
┌─────────────────┐  │         │
│Inventory Service│──┘         │ Subscribe
└─────────────────┘            │
                               ▼
                    ┌──────────────────────┐
                    │ Notification Service │
                    └──────────────────────┘
```

### Topic Strategy

**StockXpress Topics**:
- `order.placed` - Order placement events
- `order.confirmed` - Order confirmation events
- `order.cancelled` - Order cancellation events
- `inventory.updated` - Inventory level changes
- `product.created` - New product events
- `product.updated` - Product update events
- `notification.email` - Email notification requests
- `notification.sms` - SMS notification requests

### Multi-AZ Deployment

```
AZ-1              AZ-2              AZ-3
┌──────────┐      ┌──────────┐      ┌──────────┐
│ Broker 1 │      │ Broker 2 │      │ Broker 3 │
│          │      │          │      │          │
│ Leader   │──────│ Replica  │──────│ Replica  │
│ Topic A  │      │ Topic A  │      │ Topic A  │
└──────────┘      └──────────┘      └──────────┘
```

### Security Architecture

1. **Network Security**:
   - Deployed in private subnets
   - Security group restricts access to VPC CIDR
   - No public access

2. **Encryption**:
   - TLS 1.2+ for in-transit (client-broker and inter-broker)
   - AES-256 for at-rest encryption (KMS)

3. **Authentication**:
   - **SASL/SCRAM**: Username/password stored in Secrets Manager
   - **IAM**: AWS IAM-based authentication
   - Both can be enabled simultaneously

## Spring Boot Integration

### Dependencies (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### Configuration (application.yml)

#### Producer Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        max.in.flight.requests.per.connection: 5
        enable.idempotence: true
        compression.type: zstd
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: SCRAM-SHA-512
      sasl.jaas.config: org.apache.kafka.common.security.scram.ScramLoginModule required username="${KAFKA_USERNAME}" password="${KAFKA_PASSWORD}";
      ssl.truststore.location: /path/to/truststore.jks
      ssl.truststore.password: ${TRUSTSTORE_PASSWORD}
```

#### Consumer Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: com.seshrao.stockxpress.*
    listener:
      ack-mode: manual
      concurrency: 3
```

### Java Producer Example

```java
@Service
public class OrderEventPublisher {
    
    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    
    public void publishOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send("order.placed", event.getOrderId(), event)
            .addCallback(
                result -> log.info("Order event published: {}", event.getOrderId()),
                ex -> log.error("Failed to publish order event", ex)
            );
    }
}
```

### Java Consumer Example

```java
@Service
public class OrderEventConsumer {
    
    @KafkaListener(
        topics = "order.placed",
        groupId = "notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderPlaced(
            @Payload OrderPlacedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("Received order event: {} from partition: {}, offset: {}",
                event.getOrderId(), partition, offset);
            
            // Process the event
            notificationService.sendOrderConfirmation(event);
            
            // Manual commit
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing order event", e);
            // Don't acknowledge - will retry
        }
    }
}
```

### Kafka Configuration Class

```java
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "zstd");
        
        // Security settings
        config.put("security.protocol", "SASL_SSL");
        config.put("sasl.mechanism", "SCRAM-SHA-512");
        config.put("sasl.jaas.config", 
            String.format("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
                kafkaUsername, kafkaPassword));
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

## Monitoring

### CloudWatch Metrics

**Broker Metrics**:
- `CpuUser`: CPU utilization
- `KafkaDataLogsDiskUsed`: Disk usage percentage
- `MemoryUsed`: Memory consumption
- `NetworkRxPackets/NetworkTxPackets`: Network traffic

**Topic Metrics**:
- `BytesInPerSec`: Incoming bytes per second
- `BytesOutPerSec`: Outgoing bytes per second
- `MessagesInPerSec`: Messages per second
- `FetchConsumerTotalTimeMs`: Consumer fetch latency
- `ProduceTotalTimeMs`: Producer latency

**Consumer Group Metrics**:
- `SumOffsetLag`: Total lag across partitions
- `MaxOffsetLag`: Maximum lag for any partition

### Prometheus Integration

The module enables JMX and Node exporters for Prometheus:

```yaml
# Prometheus scrape config
scrape_configs:
  - job_name: 'msk-cluster'
    static_configs:
      - targets:
          - '<broker-1>:11001'  # JMX exporter
          - '<broker-2>:11001'
          - '<broker-3>:11001'
          - '<broker-1>:11002'  # Node exporter
          - '<broker-2>:11002'
          - '<broker-3>:11002'
```

### CloudWatch Alarms

- High CPU utilization (>80%)
- High disk usage (>85%)

## Best Practices

1. **Broker Count**: Use multiples of AZ count (e.g., 3, 6, 9 for 3 AZs)
2. **Replication Factor**: Use 3 for production, 2 minimum
3. **Min In-Sync Replicas**: Set to `replication_factor - 1` for durability
4. **Partitions**: More partitions = higher parallelism, but more overhead
5. **Message Size**: Keep messages small; use references for large data
6. **Consumer Groups**: One consumer group per logical application
7. **Idempotence**: Enable producer idempotence to prevent duplicates
8. **Monitoring**: Monitor consumer lag closely
9. **Security**: Always use TLS and authentication in production
10. **Capacity Planning**: Monitor disk usage and scale before hitting limits

## Topic Creation

### Using Kafka CLI

```bash
# Create topic
kafka-topics.sh --create \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --command-config client.properties \
  --topic order.placed \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config compression.type=zstd

# List topics
kafka-topics.sh --list \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --command-config client.properties

# Describe topic
kafka-topics.sh --describe \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --command-config client.properties \
  --topic order.placed
```

### Client Properties File

```properties
# client.properties
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-512
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="kafka-admin" password="<password>";
ssl.truststore.location=/path/to/kafka.client.truststore.jks
ssl.truststore.password=<password>
```

## Troubleshooting

### Connection Issues

```bash
# Test connectivity to broker
telnet <broker-endpoint> 9096

# Check security group rules
aws ec2 describe-security-groups --group-ids <sg-id>

# Verify SCRAM credentials
aws secretsmanager get-secret-value --secret-id <secret-name>
```

### Performance Issues

1. **High Producer Latency**:
   - Check `ProduceTotalTimeMs` metric
   - Increase `linger.ms` for batching
   - Enable compression
   - Increase broker count

2. **Consumer Lag**:
   - Scale consumer instances
   - Increase partition count
   - Optimize consumer processing logic
   - Check for slow consumers

3. **Disk Full**:
   - Reduce retention period
   - Enable compression
   - Increase broker volume size
   - Scale out brokers

### Common Commands

```bash
# Check consumer group lag
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --command-config client.properties \
  --group notification-service \
  --describe

# Reset consumer group offset
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --command-config client.properties \
  --group notification-service \
  --topic order.placed \
  --reset-offsets --to-earliest \
  --execute

# Produce test message
echo '{"orderId":"test-123"}' | kafka-console-producer.sh \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --producer.config client.properties \
  --topic order.placed

# Consume messages
kafka-console-consumer.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --consumer.config client.properties \
  --topic order.placed \
  --from-beginning
```

## Cost Optimization

- **Development**: 3 kafka.t3.small brokers, 50GB storage
- **Staging**: 3 kafka.m5.large brokers, 100GB storage
- **Production**: 6 kafka.m5.xlarge brokers, 500GB storage with provisioned throughput
- **Savings**: Use Graviton-based instances (kafka.m6g.*) for 20% savings

## Scaling

### Horizontal Scaling (Add Brokers)

```bash
# Update broker count
terraform apply -var="number_of_broker_nodes=6"

# Reassign partitions to new brokers (manual step)
kafka-reassign-partitions.sh --bootstrap-server $BOOTSTRAP_SERVERS \
  --command-config client.properties \
  --reassignment-json-file reassignment.json \
  --execute
```

### Vertical Scaling (Resize Brokers)

```bash
# Update instance type
terraform apply -var="broker_instance_type=kafka.m5.2xlarge"
```

### Storage Expansion

```bash
# Increase volume size (online operation)
terraform apply -var="broker_volume_size=200"
```

## References

- [AWS MSK Documentation](https://docs.aws.amazon.com/msk/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- [MSK Best Practices](https://docs.aws.amazon.com/msk/latest/developerguide/bestpractices.html)
