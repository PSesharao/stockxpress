# DocumentDB Terraform Module

This module creates an AWS DocumentDB cluster for MongoDB-compatible workloads in the StockXpress application, specifically designed for the Product Service.

## Features

- **MongoDB-Compatible**: Fully managed DocumentDB cluster compatible with MongoDB 5.0
- **High Availability**: Configurable multi-instance cluster with automatic failover
- **Security**: 
  - TLS encryption in transit
  - Encryption at rest with KMS
  - VPC security groups
  - Subnet groups for network isolation
- **Monitoring**: CloudWatch logs, metrics, and alarms
- **Backup**: Automated daily backups with configurable retention
- **Performance**: Profiler support for query optimization

## Usage

```hcl
module "documentdb" {
  source = "./modules/documentdb"
  
  project_name        = "stockxpress"
  environment         = "production"
  vpc_id              = module.vpc.vpc_id
  database_subnet_ids = module.vpc.database_subnet_ids
  
  cluster_size       = 3
  instance_class     = "db.r5.large"
  master_username    = "productadmin"
  
  backup_retention_period = 14
  deletion_protection     = true
  
  tags = {
    Project     = "StockXpress"
    Environment = "production"
    ManagedBy   = "Terraform"
  }
}
```

## Architecture

### Product Service Integration

The DocumentDB cluster is designed for the Product Service which requires:
- Flexible schema for product catalog
- Fast reads for product queries
- Document-based storage for product attributes
- Secondary indexes for search and filtering

### Security Configuration

1. **Network Security**:
   - Deployed in private database subnets
   - Security group allows access only from VPC CIDR
   - No public access

2. **Encryption**:
   - TLS required for all connections
   - Storage encryption with KMS
   - Encrypted backups

3. **Authentication**:
   - Strong random password generation
   - Credentials stored in AWS Secrets Manager

### Backup Strategy

- **Automated Backups**: Daily backups during maintenance window
- **Retention**: 7 days (default), configurable up to 35 days
- **Point-in-Time Recovery**: Enabled through continuous backups
- **Final Snapshot**: Created before cluster deletion (unless skipped)

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| project_name | Project name for resource naming | string | - | yes |
| environment | Environment name | string | - | yes |
| vpc_id | VPC ID | string | - | yes |
| database_subnet_ids | Subnet IDs for cluster | list(string) | - | yes |
| cluster_size | Number of instances | number | 1 | no |
| instance_class | Instance class | string | db.t3.medium | no |
| master_username | Master username | string | productadmin | no |
| engine_version | DocumentDB version | string | 5.0.0 | no |
| backup_retention_period | Backup retention days | number | 7 | no |
| deletion_protection | Enable deletion protection | bool | true | no |
| enable_profiler | Enable query profiler | bool | false | no |

## Outputs

| Name | Description |
|------|-------------|
| cluster_endpoint | Primary endpoint for writes |
| cluster_reader_endpoint | Reader endpoint for reads |
| cluster_port | Port number (27017) |
| master_username | Master username |
| master_password | Master password (sensitive) |
| security_group_id | Security group ID |
| connection_string | Full connection string |

## Connection Example

### Spring Boot Configuration

```yaml
spring:
  data:
    mongodb:
      uri: ${DOCUMENTDB_CONNECTION_STRING}
      database: product_catalog
      ssl:
        enabled: true
```

### Java Code

```java
@Configuration
public class DocumentDBConfig {
    @Value("${spring.data.mongodb.uri}")
    private String connectionString;
    
    @Bean
    public MongoClient mongoClient() {
        System.setProperty("javax.net.ssl.trustStore", "/path/to/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        
        return MongoClients.create(connectionString);
    }
}
```

## TLS Certificate Setup

DocumentDB requires TLS certificates for secure connections:

```bash
# Download AWS DocumentDB certificate bundle
wget https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem

# For Java applications, create a truststore
keytool -importcert -trustcacerts -file global-bundle.pem \
  -keystore documentdb-truststore.jks -storepass mypassword
```

## Monitoring

### CloudWatch Metrics

- **CPUUtilization**: CPU usage per instance
- **DatabaseConnections**: Active connections
- **FreeableMemory**: Available memory
- **WriteIOPS**: Write operations per second
- **ReadIOPS**: Read operations per second

### CloudWatch Logs

- **Audit Logs**: Connection and authentication events
- **Profiler Logs**: Slow queries (if enabled)

### Alarms

- High CPU utilization (>80%)
- High database connections (configurable threshold)

## Best Practices

1. **High Availability**: Use at least 3 instances across multiple AZs for production
2. **Instance Sizing**: Monitor metrics and adjust instance class as needed
3. **Connection Pooling**: Configure appropriate connection pool settings in application
4. **Indexing**: Create indexes for frequently queried fields
5. **Backup Testing**: Regularly test restore procedures
6. **Security**: Rotate credentials periodically
7. **Profiler**: Enable profiler in non-production to identify slow queries

## Troubleshooting

### Connection Issues

```bash
# Test connectivity from application instance
telnet <cluster-endpoint> 27017

# Verify security group rules
aws ec2 describe-security-groups --group-ids <sg-id>
```

### Performance Issues

1. Check CloudWatch metrics for CPU and memory
2. Enable profiler to identify slow queries
3. Review and optimize indexes
4. Consider scaling up instance class or adding read replicas

## Cost Optimization

- **Development**: Use single db.t3.medium instance
- **Staging**: Use 1-2 db.t3.medium instances
- **Production**: Use 3+ db.r5.large instances across AZs
- **Monitoring**: Review CloudWatch metrics to right-size instances

## Migration from MongoDB

See [AWS DocumentDB Migration Guide](https://docs.aws.amazon.com/documentdb/latest/developerguide/docdb-migration.html) for details on:
- AWS Database Migration Service (DMS)
- mongodump/mongorestore
- Change streams for live migration

## References

- [AWS DocumentDB Documentation](https://docs.aws.amazon.com/documentdb/)
- [DocumentDB Best Practices](https://docs.aws.amazon.com/documentdb/latest/developerguide/best_practices.html)
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb)
