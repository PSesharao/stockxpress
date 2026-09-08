# Complete Example - Variables

# General
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "myapp"
}

variable "environment" {
  description = "Environment name (e.g., dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "common_tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default = {
    Terraform   = "true"
    ManagedBy   = "terraform"
  }
}

# VPC Variables
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "az_count" {
  description = "Number of Availability Zones"
  type        = number
  default     = 3
}

variable "enable_nat_gateway" {
  description = "Enable NAT Gateway"
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Use single NAT Gateway (cost optimization)"
  type        = bool
  default     = false
}

variable "enable_vpc_flow_logs" {
  description = "Enable VPC Flow Logs"
  type        = bool
  default     = false
}

variable "vpc_flow_logs_retention_days" {
  description = "VPC Flow Logs retention period"
  type        = number
  default     = 7
}

# EKS Variables
variable "eks_cluster_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.28"
}

variable "eks_endpoint_private_access" {
  description = "Enable private API endpoint"
  type        = bool
  default     = true
}

variable "eks_endpoint_public_access" {
  description = "Enable public API endpoint"
  type        = bool
  default     = true
}

variable "eks_endpoint_public_access_cidrs" {
  description = "CIDR blocks allowed to access public API"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "eks_enabled_log_types" {
  description = "EKS control plane log types"
  type        = list(string)
  default     = ["api", "audit", "authenticator", "controllerManager", "scheduler"]
}

variable "eks_node_groups" {
  description = "EKS node group configurations"
  type = map(object({
    desired_size     = optional(number, 2)
    max_size         = optional(number, 4)
    min_size         = optional(number, 1)
    instance_types   = optional(list(string), ["t3.medium"])
    capacity_type    = optional(string, "ON_DEMAND")
    disk_size        = optional(number, 50)
    disk_type        = optional(string, "gp3")
    disk_iops        = optional(number, 3000)
    disk_throughput  = optional(number, 125)
    labels           = optional(map(string), {})
    taints = optional(list(object({
      key    = string
      value  = optional(string)
      effect = string
    })), [])
    max_unavailable_percentage = optional(number, 33)
    enable_monitoring          = optional(bool, true)
  }))
  default = {
    general = {
      desired_size   = 3
      min_size       = 2
      max_size       = 6
      instance_types = ["t3.large"]
      capacity_type  = "ON_DEMAND"
      disk_size      = 50
      labels = {
        role = "general"
      }
    }
  }
}

variable "enable_ebs_csi_driver" {
  description = "Enable EBS CSI driver"
  type        = bool
  default     = true
}

# RDS Variables
variable "rds_order_instance_class" {
  description = "Instance class for order database"
  type        = string
  default     = "db.t3.medium"
}

variable "rds_order_allocated_storage" {
  description = "Allocated storage for order database (GB)"
  type        = number
  default     = 100
}

variable "rds_order_max_allocated_storage" {
  description = "Max allocated storage for order database (GB)"
  type        = number
  default     = 500
}

variable "rds_inventory_instance_class" {
  description = "Instance class for inventory database"
  type        = string
  default     = "db.t3.medium"
}

variable "rds_inventory_allocated_storage" {
  description = "Allocated storage for inventory database (GB)"
  type        = number
  default     = 100
}

variable "rds_inventory_max_allocated_storage" {
  description = "Max allocated storage for inventory database (GB)"
  type        = number
  default     = 500
}

variable "rds_multi_az" {
  description = "Enable Multi-AZ for RDS"
  type        = bool
  default     = true
}

variable "rds_backup_retention_period" {
  description = "Backup retention period (days)"
  type        = number
  default     = 7
}

variable "rds_deletion_protection" {
  description = "Enable deletion protection"
  type        = bool
  default     = true
}

variable "rds_performance_insights_enabled" {
  description = "Enable Performance Insights"
  type        = bool
  default     = true
}

variable "enable_rds_cloudwatch_alarms" {
  description = "Enable CloudWatch alarms for RDS"
  type        = bool
  default     = true
}

variable "rds_alarm_actions" {
  description = "SNS topic ARNs for RDS alarms"
  type        = list(string)
  default     = []
}

variable "store_db_passwords_in_secrets_manager" {
  description = "Store database passwords in Secrets Manager"
  type        = bool
  default     = true
}

# Alerting
variable "create_sns_topic_for_alarms" {
  description = "Create SNS topic for alarms"
  type        = bool
  default     = false
}

variable "alert_email" {
  description = "Email address for alerts"
  type        = string
  default     = ""
}
