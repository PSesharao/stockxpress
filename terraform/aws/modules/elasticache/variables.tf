# ElastiCache Redis Module Variables

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, production)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where ElastiCache will be deployed"
  type        = string
}

variable "cache_subnet_ids" {
  description = "List of subnet IDs for ElastiCache subnet group"
  type        = list(string)
}

variable "node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.t3.medium"
}

variable "num_cache_nodes" {
  description = "Number of cache nodes (for non-cluster mode)"
  type        = number
  default     = 2
}

variable "engine_version" {
  description = "Redis engine version"
  type        = string
  default     = "7.0"
}

variable "parameter_group_family" {
  description = "Redis parameter group family"
  type        = string
  default     = "redis7"
}

variable "automatic_failover_enabled" {
  description = "Enable automatic failover for multi-AZ"
  type        = bool
  default     = true
}

variable "multi_az_enabled" {
  description = "Enable Multi-AZ support"
  type        = bool
  default     = true
}

variable "transit_encryption_enabled" {
  description = "Enable encryption in transit (TLS)"
  type        = bool
  default     = true
}

variable "auth_token_enabled" {
  description = "Enable Redis AUTH (requires transit encryption)"
  type        = bool
  default     = true
}

variable "redis_auth_token" {
  description = "Redis AUTH token for authentication. MUST be provided from AWS Secrets Manager. Never hardcode or commit this value."
  type        = string
  default     = null
  sensitive   = true
}

variable "kms_key_id" {
  description = "KMS key ID for encryption at rest"
  type        = string
  default     = null
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

variable "auto_minor_version_upgrade" {
  description = "Enable automatic minor version upgrades"
  type        = bool
  default     = true
}

variable "apply_immediately" {
  description = "Apply changes immediately"
  type        = bool
  default     = false
}

# Cluster mode configuration
variable "cluster_mode_enabled" {
  description = "Enable Redis cluster mode for sharding"
  type        = bool
  default     = false
}

variable "num_node_groups" {
  description = "Number of node groups (shards) for cluster mode"
  type        = number
  default     = 1
}

variable "replicas_per_node_group" {
  description = "Number of replicas per node group in cluster mode"
  type        = number
  default     = 1
}

# Redis parameters
variable "maxmemory_policy" {
  description = "Redis maxmemory eviction policy"
  type        = string
  default     = "allkeys-lru"
  
  validation {
    condition = contains([
      "volatile-lru", "allkeys-lru", "volatile-lfu", "allkeys-lfu",
      "volatile-random", "allkeys-random", "volatile-ttl", "noeviction"
    ], var.maxmemory_policy)
    error_message = "Invalid maxmemory policy."
  }
}

variable "timeout" {
  description = "Close connection after client idle for N seconds (0 = disabled)"
  type        = string
  default     = "300"
}

variable "notify_keyspace_events" {
  description = "Keyspace notifications (Ex: for expired keys)"
  type        = string
  default     = ""
}

variable "maxclients" {
  description = "Maximum number of client connections"
  type        = string
  default     = "65000"
}

# Logging
variable "enable_slow_log" {
  description = "Enable slow log to CloudWatch"
  type        = bool
  default     = true
}

variable "enable_engine_log" {
  description = "Enable engine log to CloudWatch"
  type        = bool
  default     = false
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 7
}

variable "cloudwatch_log_kms_key_id" {
  description = "KMS key ID for CloudWatch logs encryption"
  type        = string
  default     = null
}

# Monitoring
variable "enable_cloudwatch_alarms" {
  description = "Enable CloudWatch alarms for Redis"
  type        = bool
  default     = true
}

variable "alarm_actions" {
  description = "List of ARNs for alarm actions (SNS topics)"
  type        = list(string)
  default     = []
}

variable "max_connections_threshold" {
  description = "Threshold for current connections alarm"
  type        = number
  default     = 50000
}

variable "notification_topic_arn" {
  description = "SNS topic ARN for ElastiCache notifications"
  type        = string
  default     = null
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
