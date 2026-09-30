# ElastiCache Redis Cluster for Caching
# Provides managed Redis cache for improved application performance

# Input variables
variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where ElastiCache will be deployed"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for ElastiCache"
  type        = list(string)
}

variable "app_security_group_id" {
  description = "Security group ID of the application that needs to access ElastiCache"
  type        = string
}

variable "node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.t3.micro"
}

variable "num_cache_nodes" {
  description = "Number of cache nodes in the cluster"
  type        = number
  default     = 2
}

variable "parameter_group_family" {
  description = "ElastiCache parameter group family"
  type        = string
  default     = "redis7"
}

variable "engine_version" {
  description = "Redis engine version"
  type        = string
  default     = "7.0"
}

variable "port" {
  description = "Port number for ElastiCache"
  type        = number
  default     = 6379
}

variable "snapshot_retention_limit" {
  description = "Number of days to retain automatic snapshots"
  type        = number
  default     = 5
}

variable "snapshot_window" {
  description = "Daily time range for snapshots (UTC)"
  type        = string
  default     = "03:00-05:00"
}

variable "maintenance_window" {
  description = "Weekly time range for maintenance (UTC)"
  type        = string
  default     = "sun:05:00-sun:07:00"
}

variable "automatic_failover_enabled" {
  description = "Enable automatic failover for replication group"
  type        = bool
  default     = true
}

variable "multi_az_enabled" {
  description = "Enable Multi-AZ deployment"
  type        = bool
  default     = true
}

variable "at_rest_encryption_enabled" {
  description = "Enable encryption at rest"
  type        = bool
  default     = true
}

variable "transit_encryption_enabled" {
  description = "Enable encryption in transit"
  type        = bool
  default     = true
}

# SECURITY: Auth token must be provided via secure environment variable or secret manager
# Set TF_VAR_auth_token in CI/CD or retrieve from AWS Secrets Manager
variable "auth_token" {
  description = "ElastiCache authentication token supplied by the deployment environment"
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.auth_token) >= 16 && length(var.auth_token) <= 128
    error_message = "auth_token must contain between 16 and 128 characters."
  }
}

variable "tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default     = {}
}

# Subnet Group
resource "aws_elasticache_subnet_group" "main" {
  name       = "stockxpress-${var.environment}-cache-subnet"
  subnet_ids = var.private_subnet_ids

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-cache-subnet"
      Environment = var.environment
    }
  )
}

# Security Group
resource "aws_security_group" "elasticache" {
  name_prefix = "stockxpress-${var.environment}-elasticache-"
  description = "Security group for ElastiCache Redis cluster"
  vpc_id      = var.vpc_id

  # Ingress from application
  ingress {
    description     = "Redis from application"
    from_port       = var.port
    to_port         = var.port
    protocol        = "tcp"
    security_groups = [var.app_security_group_id]
  }

  # Egress
  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-elasticache"
      Environment = var.environment
    }
  )

  lifecycle {
    create_before_destroy = true
  }
}

# Parameter Group
resource "aws_elasticache_parameter_group" "main" {
  name   = "stockxpress-${var.environment}-redis-params"
  family = var.parameter_group_family

  # Custom parameters for performance and security
  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }

  parameter {
    name  = "timeout"
    value = "300"
  }

  parameter {
    name  = "tcp-keepalive"
    value = "300"
  }

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-redis-params"
      Environment = var.environment
    }
  )
}

# Replication Group (Redis Cluster)
resource "aws_elasticache_replication_group" "main" {
  replication_group_id       = "stockxpress-${var.environment}-redis"
  replication_group_description = "Redis cluster for StockXpress ${var.environment}"
  
  engine               = "redis"
  engine_version       = var.engine_version
  node_type            = var.node_type
  num_cache_clusters   = var.num_cache_nodes
  port                 = var.port
  parameter_group_name = aws_elasticache_parameter_group.main.name
  subnet_group_name    = aws_elasticache_subnet_group.main.name
  security_group_ids   = [aws_security_group.elasticache.id]

  # High Availability
  automatic_failover_enabled = var.automatic_failover_enabled
  multi_az_enabled          = var.multi_az_enabled

  # Security
  at_rest_encryption_enabled = var.at_rest_encryption_enabled
  transit_encryption_enabled = var.transit_encryption_enabled
  
  # SECURITY: Use secure auth token from variable (injected via CI/CD or secret manager)
  auth_token = var.auth_token

  # Backup and Maintenance
  snapshot_retention_limit = var.snapshot_retention_limit
  snapshot_window         = var.snapshot_window
  maintenance_window      = var.maintenance_window
  auto_minor_version_upgrade = true

  # Notifications (optional - configure SNS topic separately)
  # notification_topic_arn = aws_sns_topic.elasticache_events.arn

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-redis"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )

  lifecycle {
    ignore_changes = [
      # Ignore changes to engine_version to prevent unintended upgrades
      engine_version,
    ]
  }
}

# CloudWatch Alarms for monitoring
resource "aws_cloudwatch_metric_alarm" "cache_cpu" {
  alarm_name          = "stockxpress-${var.environment}-redis-cpu-utilization"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ElastiCache"
  period              = "300"
  statistic           = "Average"
  threshold           = "75"
  alarm_description   = "This metric monitors ElastiCache CPU utilization"
  alarm_actions       = [] # Add SNS topic ARN for notifications

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.main.id
  }

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-redis-cpu-alarm"
      Environment = var.environment
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "cache_memory" {
  alarm_name          = "stockxpress-${var.environment}-redis-memory-utilization"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "DatabaseMemoryUsagePercentage"
  namespace           = "AWS/ElastiCache"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors ElastiCache memory utilization"
  alarm_actions       = [] # Add SNS topic ARN for notifications

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.main.id
  }

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-redis-memory-alarm"
      Environment = var.environment
    }
  )
}

# Outputs
output "redis_endpoint" {
  description = "Primary endpoint for the Redis cluster"
  value       = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "redis_port" {
  description = "Port for the Redis cluster"
  value       = var.port
}

output "redis_reader_endpoint" {
  description = "Reader endpoint for the Redis cluster"
  value       = aws_elasticache_replication_group.main.reader_endpoint_address
}

output "redis_security_group_id" {
  description = "Security group ID for ElastiCache"
  value       = aws_security_group.elasticache.id
}

output "replication_group_id" {
  description = "ID of the ElastiCache replication group"
  value       = aws_elasticache_replication_group.main.id
}
