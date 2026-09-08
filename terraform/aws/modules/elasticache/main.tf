# ElastiCache Redis Cluster for Caching
# Provides managed Redis cache for Inventory Service and other services

# SECURITY: Auth token should be managed externally via AWS Secrets Manager
# DO NOT generate passwords in Terraform - use data source to retrieve from Secrets Manager
# Example: data "aws_secretsmanager_secret_version" "redis_auth_token" {...}

# Security Group for ElastiCache Redis
resource "aws_security_group" "redis" {
  name_prefix = "${var.project_name}-${var.environment}-redis-"
  description = "Security group for ElastiCache Redis cluster"
  vpc_id      = var.vpc_id

  ingress {
    description = "Redis from VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-redis-sg"
    }
  )

  lifecycle {
    create_before_destroy = true
  }
}

# Subnet Group for ElastiCache
resource "aws_elasticache_subnet_group" "redis" {
  name        = "${var.project_name}-${var.environment}-redis-subnet-group"
  description = "Subnet group for ElastiCache Redis cluster"
  subnet_ids  = var.cache_subnet_ids

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-redis-subnet-group"
    }
  )
}

# Parameter Group for ElastiCache Redis
resource "aws_elasticache_parameter_group" "redis" {
  family      = var.parameter_group_family
  name        = "${var.project_name}-${var.environment}-redis-params"
  description = "ElastiCache Redis parameter group for ${var.project_name}"

  # Eviction policy - LRU for cache
  parameter {
    name  = "maxmemory-policy"
    value = var.maxmemory_policy
  }

  # Timeout for idle connections
  parameter {
    name  = "timeout"
    value = var.timeout
  }

  # TCP keepalive
  parameter {
    name  = "tcp-keepalive"
    value = "300"
  }

  # Notify keyspace events for expired keys
  dynamic "parameter" {
    for_each = var.notify_keyspace_events != "" ? [1] : []
    content {
      name  = "notify-keyspace-events"
      value = var.notify_keyspace_events
    }
  }

  # Max clients
  parameter {
    name  = "maxclients"
    value = var.maxclients
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-redis-params"
    }
  )
}

# ElastiCache Replication Group (Redis Cluster)
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "${var.project_name}-${var.environment}-redis"
  replication_group_description = "Redis cluster for ${var.project_name} ${var.environment}"
  engine                     = "redis"
  engine_version             = var.engine_version
  node_type                  = var.node_type
  num_cache_clusters         = var.cluster_mode_enabled ? null : var.num_cache_nodes
  parameter_group_name       = aws_elasticache_parameter_group.redis.name
  subnet_group_name          = aws_elasticache_subnet_group.redis.name
  security_group_ids         = [aws_security_group.redis.id]
  port                       = 6379
  
  # Multi-AZ with automatic failover
  automatic_failover_enabled = var.automatic_failover_enabled
  multi_az_enabled           = var.multi_az_enabled
  
  # Auth and encryption
  at_rest_encryption_enabled = true
  transit_encryption_enabled = var.transit_encryption_enabled
  auth_token_enabled         = var.auth_token_enabled
  # SECURITY: auth_token must be provided via variable from AWS Secrets Manager
  # Never hardcode or generate in Terraform. Reference external secret:
  # auth_token = data.aws_secretsmanager_secret_version.redis_auth.secret_string
  auth_token                 = var.auth_token_enabled ? var.redis_auth_token : null
  kms_key_id                 = var.kms_key_id
  
  # Backup and maintenance
  snapshot_retention_limit   = var.snapshot_retention_limit
  snapshot_window            = var.snapshot_window
  maintenance_window         = var.maintenance_window
  auto_minor_version_upgrade = var.auto_minor_version_upgrade
  
  # Cluster mode configuration
  dynamic "cluster_mode" {
    for_each = var.cluster_mode_enabled ? [1] : []
    content {
      num_node_groups         = var.num_node_groups
      replicas_per_node_group = var.replicas_per_node_group
    }
  }
  
  # Logging
  dynamic "log_delivery_configuration" {
    for_each = var.enable_slow_log ? [1] : []
    content {
      destination      = aws_cloudwatch_log_group.redis_slow_log[0].name
      destination_type = "cloudwatch-logs"
      log_format       = "json"
      log_type         = "slow-log"
    }
  }
  
  dynamic "log_delivery_configuration" {
    for_each = var.enable_engine_log ? [1] : []
    content {
      destination      = aws_cloudwatch_log_group.redis_engine_log[0].name
      destination_type = "cloudwatch-logs"
      log_format       = "json"
      log_type         = "engine-log"
    }
  }
  
  # Notifications
  notification_topic_arn = var.notification_topic_arn
  
  apply_immediately = var.apply_immediately
  
  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-redis"
    }
  )
}

# CloudWatch Log Groups for Redis logs
resource "aws_cloudwatch_log_group" "redis_slow_log" {
  count             = var.enable_slow_log ? 1 : 0
  name              = "/aws/elasticache/${var.project_name}-${var.environment}-redis/slow-log"
  retention_in_days = var.log_retention_days
  kms_key_id        = var.cloudwatch_log_kms_key_id

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-redis-slow-log"
    }
  )
}

resource "aws_cloudwatch_log_group" "redis_engine_log" {
  count             = var.enable_engine_log ? 1 : 0
  name              = "/aws/elasticache/${var.project_name}-${var.environment}-redis/engine-log"
  retention_in_days = var.log_retention_days
  kms_key_id        = var.cloudwatch_log_kms_key_id

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-redis-engine-log"
    }
  )
}

# CloudWatch Alarms for Redis
resource "aws_cloudwatch_metric_alarm" "redis_cpu" {
  count               = var.enable_cloudwatch_alarms ? 1 : 0
  alarm_name          = "${var.project_name}-${var.environment}-redis-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ElastiCache"
  period              = "300"
  statistic           = "Average"
  threshold           = "75"
  alarm_description   = "This metric monitors Redis CPU utilization"
  alarm_actions       = var.alarm_actions

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.redis.id
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "redis_memory" {
  count               = var.enable_cloudwatch_alarms ? 1 : 0
  alarm_name          = "${var.project_name}-${var.environment}-redis-memory"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "DatabaseMemoryUsagePercentage"
  namespace           = "AWS/ElastiCache"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors Redis memory usage"
  alarm_actions       = var.alarm_actions

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.redis.id
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "redis_evictions" {
  count               = var.enable_cloudwatch_alarms ? 1 : 0
  alarm_name          = "${var.project_name}-${var.environment}-redis-evictions"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "Evictions"
  namespace           = "AWS/ElastiCache"
  period              = "300"
  statistic           = "Sum"
  threshold           = "100"
  alarm_description   = "This metric monitors Redis evictions"
  alarm_actions       = var.alarm_actions

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.redis.id
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "redis_connections" {
  count               = var.enable_cloudwatch_alarms ? 1 : 0
  alarm_name          = "${var.project_name}-${var.environment}-redis-connections"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CurrConnections"
  namespace           = "AWS/ElastiCache"
  period              = "300"
  statistic           = "Average"
  threshold           = var.max_connections_threshold
  alarm_description   = "This metric monitors Redis current connections"
  alarm_actions       = var.alarm_actions

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.redis.id
  }

  tags = var.tags
}

# Data source for VPC
data "aws_vpc" "selected" {
  id = var.vpc_id
}
