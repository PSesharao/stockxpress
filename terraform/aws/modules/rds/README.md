# RDS MySQL Module

This module creates production-ready MySQL RDS instances with automated backups, encryption, Multi-AZ support, enhanced monitoring, and CloudWatch alarms.

## Features

- ✅ MySQL RDS instances with configurable storage and compute
- ✅ KMS encryption for data at rest
- ✅ Multi-AZ deployment for high availability
- ✅ Automated backups with configurable retention
- ✅ Enhanced monitoring with CloudWatch metrics
- ✅ Performance Insights enabled
- ✅ Security groups with configurable access
- ✅ Secrets Manager integration for credentials
- ✅ CloudWatch alarms for CPU, storage, and memory
- ✅ Custom parameter and option groups
- ✅ Storage autoscaling
- ✅ Deletion protection

## Usage

### Basic Example - Two Databases (Order & Inventory)

```hcl
module "rds" {
  source = "./modules/rds"

  name_prefix          = "myapp"
  vpc_id               = module.vpc.vpc_id
  db_subnet_group_name = module.vpc.db_subnet_group_name

  databases = {
    order = {
      database_name       = "orders"
      instance_class      = "db.r6g.xlarge"
      allocated_storage   = 200
      multi_az            = true
      allowed_security_groups = [module.eks.node_security_group_id]
    }
    
    inventory = {
      database_name       = "inventory"
      instance_class      = "db.r6g.large"
      allocated_storage   = 100
      multi_az            = true
      allowed_security_groups = [module.eks.node_security_group_id]
    }
  }

  # CloudWatch alarms
  enable_cloudwatch_alarms = true
  alarm_actions            = [aws_sns_topic.alerts.arn]

  tags = {
    Environment = "production"
    Project     = "my-project"
    Terraform   = "true"
  }
}
```

### Advanced Example - Custom Configuration

```hcl
module "rds" {
  source = "./modules/rds"

  name_prefix          = "myapp"
  vpc_id               = module.vpc.vpc_id
  db_subnet_group_name = module.vpc.db_subnet_group_name

  databases = {
    order = {
      database_name           = "orders"
      engine_version          = "8.0.35"
      instance_class          = "db.r6g.2xlarge"
      allocated_storage       = 500
      max_allocated_storage   = 2000
      storage_type            = "gp3"
      storage_throughput      = 250
      multi_az                = true
      backup_retention_period = 14
      deletion_protection     = true
      
      # Custom parameters
      parameters = [
        {
          name  = "max_connections"
          value = "2000"
        },
        {
          name  = "innodb_buffer_pool_size"
          value = "{DBInstanceClassMemory*3/4}"
          apply_method = "pending-reboot"
        }
      ]
      
      # Network access
      allowed_security_groups = [
        module.eks.node_security_group_id,
        aws_security_group.bastion.id
      ]
      
      # Monitoring
      performance_insights_enabled = true
      performance_insights_retention_period = 31
      monitoring_interval = 30
      
      # Alarms
      cpu_alarm_threshold     = 75
      storage_alarm_threshold = 21474836480  # 20 GB
      memory_alarm_threshold  = 2147483648   # 2 GB
    }
  }

  tags = {
    Environment = "production"
  }
}
```

## Retrieving Database Credentials

Credentials are automatically stored in AWS Secrets Manager:

```bash
# Get secret ARN from Terraform output
terraform output rds_secrets

# Retrieve credentials
aws secretsmanager get-secret-value \
  --secret-id <secret-arn> \
  --query SecretString \
  --output text | jq .
```

In application code:
```python
import boto3
import json

client = boto3.client('secretsmanager')
response = client.get_secret_value(SecretId='myapp-order-password-xxx')
secret = json.loads(response['SecretString'])

# secret contains: username, password, host, port, dbname
```

## Storage Types

| Type | Use Case | IOPS | Throughput |
|------|----------|------|------------|
| gp3 | General purpose (recommended) | 3,000-16,000 | 125-1,000 MB/s |
| gp2 | General purpose (legacy) | 3 IOPS/GB | N/A |
| io1 | High-performance OLTP | Up to 64,000 | N/A |
| io2 | Latest high-performance | Up to 64,000 | N/A |

## Backup Strategy

- **Automated Backups**: Daily snapshots during backup window
- **Retention**: 7 days default (configurable 0-35 days)
- **Backup Window**: 03:00-04:00 UTC (configurable)
- **Maintenance Window**: Sunday 04:00-05:00 UTC (configurable)
- **Final Snapshot**: Created on deletion (unless disabled)
- **Point-in-Time Recovery**: Enabled automatically

## Multi-AZ Deployment

When `multi_az = true`:
- Synchronous replication to standby instance
- Automatic failover (typically 60-120 seconds)
- No performance impact (separate infrastructure)
- Recommended for production workloads

## Monitoring & Alarms

### Built-in CloudWatch Alarms

1. **High CPU**: Triggers when CPU > 80% (configurable)
2. **Low Storage**: Triggers when free space < 10 GB
3. **Low Memory**: Triggers when freeable memory < 1 GB

### Enhanced Monitoring

- **Interval**: 60 seconds (configurable: 0, 1, 5, 10, 15, 30, 60)
- **Metrics**: 50+ OS-level metrics
- **Granularity**: Real-time process and thread information

### Performance Insights

- **Retention**: 7 days free, up to 731 days (paid)
- **Database Load**: Visualize active sessions
- **Top SQL**: Identify problematic queries

## Security

### Encryption

- **At Rest**: KMS encryption (automatic key rotation)
- **In Transit**: SSL/TLS enforced (configure in parameter group)
- **Secrets**: Master password in Secrets Manager with KMS

### Network Security

- Security groups control access
- Deploy in private database subnets
- `publicly_accessible = false` (default)
- Supports both CIDR blocks and security group sources

### Access Control

```hcl
allowed_cidr_blocks = ["10.0.0.0/8"]  # CIDR-based access
allowed_security_groups = ["sg-xxx"]   # Security group-based access
```

## Cost Optimization

### Development/Test Environments

```hcl
databases = {
  order = {
    instance_class          = "db.t3.medium"  # Burstable instances
    multi_az                = false            # Single AZ
    backup_retention_period = 3                # Shorter retention
    deletion_protection     = false            # Allow easy deletion
    performance_insights_enabled = false       # Disable PI
  }
}
```

### Production Optimization

- Use Reserved Instances for predictable workloads (up to 69% savings)
- Enable storage autoscaling to avoid over-provisioning
- Use gp3 instead of gp2 (20% cheaper, better performance)
- Right-size instances based on CloudWatch metrics

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| name_prefix | Prefix for resource names | string | - | yes |
| vpc_id | VPC ID | string | - | yes |
| db_subnet_group_name | DB subnet group name | string | - | yes |
| databases | Map of database configurations | map(object) | See variables.tf | yes |
| default_engine | Default engine | string | "mysql" | no |
| default_engine_version | Default engine version | string | "8.0.35" | no |
| default_instance_class | Default instance class | string | "db.t3.medium" | no |
| default_allocated_storage | Default storage (GB) | number | 100 | no |
| default_multi_az | Default Multi-AZ | bool | true | no |
| default_backup_retention_period | Backup retention (days) | number | 7 | no |
| enable_cloudwatch_alarms | Enable alarms | bool | true | no |
| store_master_password_in_secrets_manager | Store password in Secrets Manager | bool | true | no |
| tags | Additional tags | map(string) | {} | no |

## Outputs

| Name | Description |
|------|-------------|
| db_instance_endpoints | Database endpoints |
| db_instance_addresses | Database addresses |
| db_instance_ports | Database ports |
| db_security_group_ids | Security group IDs |
| secrets_manager_secret_arns | Secrets Manager ARNs |
| kms_key_arns | KMS key ARNs |
| connection_strings | Full connection strings (sensitive) |

## Disaster Recovery

### Automated Backups
- Retained for 7 days (configurable)
- Point-in-time recovery to any second
- Stored in S3 (managed by AWS)

### Manual Snapshots
```bash
aws rds create-db-snapshot \
  --db-instance-identifier myapp-order \
  --db-snapshot-identifier myapp-order-$(date +%Y%m%d)
```

### Cross-Region Replication
For disaster recovery, consider:
- Read replicas in other regions
- Automated snapshot copying (configure separately)

## Requirements

| Name | Version |
|------|---------|  
| terraform | >= 1.0 |
| aws | >= 5.0 |
| random | >= 3.0 |

## Maintenance

### Applying Changes

- Most parameter changes: Applied immediately or during maintenance window
- Instance class changes: Require downtime (Multi-AZ minimizes this)
- Storage scaling: Applied immediately (no downtime)
- Engine upgrades: Use `apply_immediately = false` for maintenance window

### Monitoring Recommendations

1. Set up SNS topic for alarm notifications
2. Monitor slow query logs in CloudWatch
3. Review Performance Insights weekly
4. Enable Enhanced Monitoring for production
5. Set up automated backup verification
