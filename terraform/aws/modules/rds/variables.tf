# RDS Module - Variables

variable "name_prefix" {
  description = "Prefix for naming RDS resources"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where RDS instances will be deployed"
  type        = string
}

variable "db_subnet_group_name" {
  description = "Name of DB subnet group"
  type        = string
}

variable "databases" {
  description = "Map of database configurations"
  type = map(object({
    database_name                  = optional(string)
    engine                         = optional(string)
    engine_version                 = optional(string)
    instance_class                 = optional(string)
    allocated_storage              = optional(number)
    max_allocated_storage          = optional(number)
    storage_type                   = optional(string)
    storage_encrypted              = optional(bool, true)
    iops                           = optional(number)
    storage_throughput             = optional(number)
    multi_az                       = optional(bool)
    master_username                = optional(string)
    port                           = optional(number, 3306)
    publicly_accessible            = optional(bool, false)
    backup_retention_period        = optional(number)
    backup_window                  = optional(string)
    maintenance_window             = optional(string)
    skip_final_snapshot            = optional(bool)
    enabled_cloudwatch_logs_exports = optional(list(string))
    monitoring_interval            = optional(number)
    performance_insights_enabled   = optional(bool)
    performance_insights_retention_period = optional(number)
    auto_minor_version_upgrade     = optional(bool)
    deletion_protection            = optional(bool)
    apply_immediately              = optional(bool, false)
    parameter_group_family         = optional(string)
    major_engine_version           = optional(string)
    parameters = optional(list(object({
      name         = string
      value        = string
      apply_method = optional(string, "immediate")
    })))
    options = optional(list(object({
      option_name = string
      option_settings = optional(list(object({
        name  = string
        value = string
      })))
    })))
    allowed_cidr_blocks      = optional(list(string))
    allowed_security_groups  = optional(list(string), [])
    cpu_alarm_threshold      = optional(number)
    storage_alarm_threshold  = optional(number)
    memory_alarm_threshold   = optional(number)
  }))
  default = {
    order = {
      database_name = "orders"
    }
    inventory = {
      database_name = "inventory"
    }
  }
}

# Default configuration values
variable "default_engine" {
  description = "Default database engine"
  type        = string
  default     = "mysql"
}

variable "default_engine_version" {
  description = "Default database engine version"
  type        = string
  default     = "8.0.35"
}

variable "default_major_engine_version" {
  description = "Default major engine version for option group"
  type        = string
  default     = "8.0"
}

variable "default_parameter_group_family" {
  description = "Default parameter group family"
  type        = string
  default     = "mysql8.0"
}

variable "default_instance_class" {
  description = "Default RDS instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "default_allocated_storage" {
  description = "Default allocated storage in GB"
  type        = number
  default     = 100
}

variable "default_max_allocated_storage" {
  description = "Default maximum allocated storage for autoscaling in GB"
  type        = number
  default     = 500
}

variable "default_storage_type" {
  description = "Default storage type"
  type        = string
  default     = "gp3"

  validation {
    condition     = contains(["gp2", "gp3", "io1", "io2"], var.default_storage_type)
    error_message = "Storage type must be one of: gp2, gp3, io1, io2."
  }
}

variable "default_multi_az" {
  description = "Default Multi-AZ deployment setting"
  type        = bool
  default     = true
}

variable "default_master_username" {
  description = "Default master username"
  type        = string
  default     = "admin"
}

variable "default_backup_retention_period" {
  description = "Default backup retention period in days"
  type        = number
  default     = 7

  validation {
    condition     = var.default_backup_retention_period >= 0 && var.default_backup_retention_period <= 35
    error_message = "Backup retention period must be between 0 and 35 days."
  }
}

variable "default_backup_window" {
  description = "Default preferred backup window"
  type        = string
  default     = "03:00-04:00"
}

variable "default_maintenance_window" {
  description = "Default preferred maintenance window"
  type        = string
  default     = "sun:04:00-sun:05:00"
}

variable "default_skip_final_snapshot" {
  description = "Default skip final snapshot setting"
  type        = bool
  default     = false
}

variable "default_enabled_cloudwatch_logs_exports" {
  description = "Default CloudWatch log types to export"
  type        = list(string)
  default     = ["error", "general", "slowquery"]
}

variable "default_monitoring_interval" {
  description = "Default enhanced monitoring interval in seconds (0, 1, 5, 10, 15, 30, 60)"
  type        = number
  default     = 60

  validation {
    condition     = contains([0, 1, 5, 10, 15, 30, 60], var.default_monitoring_interval)
    error_message = "Monitoring interval must be one of: 0, 1, 5, 10, 15, 30, 60."
  }
}

variable "default_performance_insights_enabled" {
  description = "Default Performance Insights setting"
  type        = bool
  default     = true
}

variable "default_performance_insights_retention_period" {
  description = "Default Performance Insights retention period in days"
  type        = number
  default     = 7

  validation {
    condition     = contains([7, 31, 62, 93, 124, 155, 186, 217, 248, 279, 310, 341, 372, 403, 434, 465, 496, 527, 558, 589, 620, 651, 682, 713, 731], var.default_performance_insights_retention_period)
    error_message = "Performance Insights retention period must be 7 days (free tier) or a multiple of 31 up to 731 days."
  }
}

variable "default_auto_minor_version_upgrade" {
  description = "Default auto minor version upgrade setting"
  type        = bool
  default     = true
}

variable "default_deletion_protection" {
  description = "Default deletion protection setting"
  type        = bool
  default     = true
}

variable "default_allowed_cidr_blocks" {
  description = "Default list of CIDR blocks allowed to access RDS"
  type        = list(string)
  default     = []
}

variable "default_parameters" {
  description = "Default database parameters"
  type = list(object({
    name         = string
    value        = string
    apply_method = optional(string, "immediate")
  }))
  default = [
    {
      name  = "character_set_server"
      value = "utf8mb4"
    },
    {
      name  = "collation_server"
      value = "utf8mb4_unicode_ci"
    },
    {
      name  = "max_connections"
      value = "1000"
    },
    {
      name  = "slow_query_log"
      value = "1"
    },
    {
      name  = "long_query_time"
      value = "2"
    },
  ]
}

# CloudWatch Alarms
variable "enable_cloudwatch_alarms" {
  description = "Enable CloudWatch alarms for RDS instances"
  type        = bool
  default     = true
}

variable "alarm_actions" {
  description = "List of ARNs to notify when alarm triggers"
  type        = list(string)
  default     = []
}

variable "default_cpu_alarm_threshold" {
  description = "Default CPU utilization threshold for alarms (percentage)"
  type        = number
  default     = 80
}

variable "default_storage_alarm_threshold" {
  description = "Default free storage space threshold for alarms (bytes)"
  type        = number
  default     = 10737418240 # 10 GB in bytes
}

variable "default_memory_alarm_threshold" {
  description = "Default freeable memory threshold for alarms (bytes)"
  type        = number
  default     = 1073741824 # 1 GB in bytes
}

# Secrets Manager
variable "store_master_password_in_secrets_manager" {
  description = "Store master password in AWS Secrets Manager"
  type        = bool
  default     = true
}

variable "secret_recovery_window_days" {
  description = "Number of days to retain secret after deletion"
  type        = number
  default     = 30
}

# KMS
variable "kms_key_deletion_window" {
  description = "KMS key deletion window in days"
  type        = number
  default     = 30
}

variable "tags" {
  description = "A map of tags to add to all resources"
  type        = map(string)
  default     = {}
}
