# Secrets Manager Module Variables

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, production)"
  type        = string
}

# Encryption
variable "create_kms_key" {
  description = "Create KMS key for secrets encryption"
  type        = bool
  default     = true
}

variable "kms_key_id" {
  description = "KMS key ID for encryption (if not creating new key)"
  type        = string
  default     = null
}

variable "recovery_window_in_days" {
  description = "Recovery window in days for deleted secrets"
  type        = number
  default     = 30
}

# RDS Order Database
variable "rds_order_username" {
  description = "RDS Order database username"
  type        = string
  default     = "orderadmin"
}

variable "rds_order_password" {
  description = "RDS Order database password"
  type        = string
  sensitive   = true
}

variable "rds_order_endpoint" {
  description = "RDS Order database endpoint"
  type        = string
  default     = ""
}

variable "rds_order_port" {
  description = "RDS Order database port"
  type        = number
  default     = 3306
}

variable "rds_order_database" {
  description = "RDS Order database name"
  type        = string
  default     = "order_service"
}

# RDS Inventory Database
variable "rds_inventory_username" {
  description = "RDS Inventory database username"
  type        = string
  default     = "inventoryadmin"
}

variable "rds_inventory_password" {
  description = "RDS Inventory database password"
  type        = string
  sensitive   = true
}

variable "rds_inventory_endpoint" {
  description = "RDS Inventory database endpoint"
  type        = string
  default     = ""
}

variable "rds_inventory_port" {
  description = "RDS Inventory database port"
  type        = number
  default     = 3306
}

variable "rds_inventory_database" {
  description = "RDS Inventory database name"
  type        = string
  default     = "inventory_service"
}

# DocumentDB
variable "documentdb_username" {
  description = "DocumentDB username"
  type        = string
  default     = "productadmin"
}

variable "documentdb_password" {
  description = "DocumentDB password"
  type        = string
  sensitive   = true
}

variable "documentdb_endpoint" {
  description = "DocumentDB cluster endpoint"
  type        = string
  default     = ""
}

variable "documentdb_port" {
  description = "DocumentDB port"
  type        = number
  default     = 27017
}

variable "documentdb_database" {
  description = "DocumentDB database name"
  type        = string
  default     = "product_catalog"
}

# ElastiCache Redis
variable "elasticache_auth_token" {
  description = "ElastiCache Redis auth token"
  type        = string
  sensitive   = true
}

variable "elasticache_endpoint" {
  description = "ElastiCache Redis endpoint"
  type        = string
  default     = ""
}

variable "elasticache_port" {
  description = "ElastiCache Redis port"
  type        = number
  default     = 6379
}

# JWT Secret
variable "create_jwt_secret" {
  description = "Create JWT secret for API Gateway"
  type        = bool
  default     = true
}

# Keycloak
variable "create_keycloak_secret" {
  description = "Create Keycloak admin credentials"
  type        = bool
  default     = true
}

variable "keycloak_admin_username" {
  description = "Keycloak admin username"
  type        = string
  default     = "admin"
}

# Email/SMTP
variable "create_email_secret" {
  description = "Create email SMTP credentials"
  type        = bool
  default     = false
}

variable "smtp_host" {
  description = "SMTP server host"
  type        = string
  default     = ""
}

variable "smtp_port" {
  description = "SMTP server port"
  type        = number
  default     = 587
}

variable "smtp_username" {
  description = "SMTP username"
  type        = string
  default     = ""
}

variable "smtp_password" {
  description = "SMTP password"
  type        = string
  sensitive   = true
  default     = ""
}

# Rotation
variable "enable_rotation" {
  description = "Enable automatic password rotation"
  type        = bool
  default     = false
}

# Resource Policy
variable "enable_resource_policy" {
  description = "Enable resource policy for secrets"
  type        = bool
  default     = false
}

variable "eks_service_account_roles" {
  description = "List of EKS service account role ARNs allowed to access secrets"
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
