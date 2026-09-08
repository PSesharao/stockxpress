# DocumentDB Module Variables

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, production)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where DocumentDB will be deployed"
  type        = string
}

variable "database_subnet_ids" {
  description = "List of subnet IDs for DocumentDB subnet group"
  type        = list(string)
}

variable "cluster_size" {
  description = "Number of instances in the DocumentDB cluster"
  type        = number
  default     = 1
}

variable "instance_class" {
  description = "Instance class for DocumentDB instances"
  type        = string
  default     = "db.t3.medium"
}

variable "master_username" {
  description = "Master username for DocumentDB"
  type        = string
  default     = "productadmin"
}

variable "master_password" {
  description = "Master password for DocumentDB. MUST be provided from AWS Secrets Manager. Never hardcode or commit this value."
  type        = string
  default     = null
  sensitive   = true
}

variable "engine_version" {
  description = "DocumentDB engine version"
  type        = string
  default     = "5.0.0"
}

variable "cluster_family" {
  description = "DocumentDB cluster parameter group family"
  type        = string
  default     = "docdb5.0"
}

variable "backup_retention_period" {
  description = "Number of days to retain backups"
  type        = number
  default     = 7
}

variable "preferred_backup_window" {
  description = "Daily time range for backups (UTC)"
  type        = string
  default     = "03:00-04:00"
}

variable "preferred_maintenance_window" {
  description = "Weekly time range for maintenance (UTC)"
  type        = string
  default     = "sun:04:00-sun:05:00"
}

variable "skip_final_snapshot" {
  description = "Skip final snapshot when destroying cluster"
  type        = bool
  default     = false
}

variable "storage_encrypted" {
  description = "Enable storage encryption"
  type        = bool
  default     = true
}

variable "kms_key_id" {
  description = "KMS key ID for encryption (uses default if not specified)"
  type        = string
  default     = null
}

variable "enabled_cloudwatch_logs_exports" {
  description = "List of log types to export to CloudWatch"
  type        = list(string)
  default     = ["audit", "profiler"]
}

variable "deletion_protection" {
  description = "Enable deletion protection"
  type        = bool
  default     = true
}

variable "apply_immediately" {
  description = "Apply changes immediately"
  type        = bool
  default     = false
}

variable "auto_minor_version_upgrade" {
  description = "Enable automatic minor version upgrades"
  type        = bool
  default     = true
}

variable "enable_profiler" {
  description = "Enable DocumentDB profiler"
  type        = bool
  default     = false
}

variable "profiler_threshold_ms" {
  description = "Profiler threshold in milliseconds"
  type        = string
  default     = "100"
}

variable "enable_cloudwatch_alarms" {
  description = "Enable CloudWatch alarms for DocumentDB"
  type        = bool
  default     = true
}

variable "alarm_actions" {
  description = "List of ARNs for alarm actions (SNS topics)"
  type        = list(string)
  default     = []
}

variable "max_connections_threshold" {
  description = "Threshold for database connections alarm"
  type        = number
  default     = 100
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
