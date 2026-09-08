# ECR Module Variables

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, production)"
  type        = string
}

variable "repositories" {
  description = "List of repository names to create"
  type        = list(string)
}

# Repository Configuration
variable "image_tag_mutability" {
  description = "Image tag mutability setting (MUTABLE or IMMUTABLE)"
  type        = string
  default     = "MUTABLE"
  
  validation {
    condition     = contains(["MUTABLE", "IMMUTABLE"], var.image_tag_mutability)
    error_message = "Must be MUTABLE or IMMUTABLE."
  }
}

variable "scan_on_push" {
  description = "Enable image scanning on push"
  type        = bool
  default     = true
}

# Encryption
variable "create_kms_key" {
  description = "Create KMS key for ECR encryption"
  type        = bool
  default     = true
}

variable "kms_key_arn" {
  description = "KMS key ARN for encryption (if not creating new key)"
  type        = string
  default     = null
}

# Lifecycle Policy
variable "max_image_count" {
  description = "Maximum number of images to keep for each tag prefix"
  type        = number
  default     = 30
}

variable "untagged_image_days" {
  description = "Days to keep untagged images"
  type        = number
  default     = 7
}

variable "keep_production_images" {
  description = "Keep production tagged images longer"
  type        = bool
  default     = true
}

variable "max_production_image_count" {
  description = "Maximum number of production images to keep"
  type        = number
  default     = 100
}

variable "tag_prefix_list" {
  description = "List of tag prefixes to match for lifecycle policy"
  type        = list(string)
  default     = ["dev-", "staging-", "v", "latest"]
}

# Cross-Account Access
variable "enable_cross_account_access" {
  description = "Enable cross-account access to repositories"
  type        = bool
  default     = false
}

variable "cross_account_principals" {
  description = "List of AWS account principals for cross-account access"
  type        = list(string)
  default     = []
}

# Replication
variable "enable_replication" {
  description = "Enable ECR replication"
  type        = bool
  default     = false
}

variable "replication_regions" {
  description = "List of regions to replicate to"
  type        = list(string)
  default     = []
}

variable "replication_repository_filter" {
  description = "Repository filter for replication (prefix match)"
  type        = string
  default     = "*"
}

# Scanning
variable "enable_enhanced_scanning" {
  description = "Enable enhanced scanning (Inspector)"
  type        = bool
  default     = false
}

variable "enhanced_scan_frequency" {
  description = "Scan frequency for enhanced scanning (SCAN_ON_PUSH, CONTINUOUS_SCAN, MANUAL)"
  type        = string
  default     = "SCAN_ON_PUSH"
  
  validation {
    condition     = contains(["SCAN_ON_PUSH", "CONTINUOUS_SCAN", "MANUAL"], var.enhanced_scan_frequency)
    error_message = "Must be SCAN_ON_PUSH, CONTINUOUS_SCAN, or MANUAL."
  }
}

variable "enable_scan_notifications" {
  description = "Enable notifications for critical scan findings"
  type        = bool
  default     = false
}

variable "scan_notification_topic_arn" {
  description = "SNS topic ARN for scan notifications"
  type        = string
  default     = null
}

# Registry Policy
variable "enable_registry_policy" {
  description = "Enable registry-level policy"
  type        = bool
  default     = false
}

variable "organization_id" {
  description = "AWS Organization ID for registry policy"
  type        = string
  default     = null
}

# Logging
variable "enable_api_logging" {
  description = "Enable CloudWatch logging for ECR API calls"
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

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
