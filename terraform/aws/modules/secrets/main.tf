# AWS Secrets Manager Configuration
# Manages application secrets securely
# SECURITY: This module manages secret metadata only - actual secret values must be injected via AWS Secrets Manager console or CLI

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default     = {}
}

# Database credentials secret
resource "aws_secretsmanager_secret" "db_credentials" {
  name_prefix             = "stockxpress/${var.environment}/db-credentials-"
  description             = "Database credentials for StockXpress ${var.environment}"
  recovery_window_in_days = 7

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-db-credentials"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )
}

# SECURITY: Do not commit actual secret values
# Secret values must be set using AWS Secrets Manager console, AWS CLI, or CI/CD pipeline
# Example CLI command to set the secret:
# aws secretsmanager put-secret-value \
#   --secret-id <secret-id> \
#   --secret-string '{"username":"admin","password":"generated-secure-password"}'

# Redis auth token secret
resource "aws_secretsmanager_secret" "redis_auth_token" {
  name_prefix             = "stockxpress/${var.environment}/redis-auth-token-"
  description             = "Redis authentication token for StockXpress ${var.environment}"
  recovery_window_in_days = 7

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-redis-auth-token"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )
}

# API Keys secret
resource "aws_secretsmanager_secret" "api_keys" {
  name_prefix             = "stockxpress/${var.environment}/api-keys-"
  description             = "External API keys for StockXpress ${var.environment}"
  recovery_window_in_days = 7

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-api-keys"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )
}

# JWT signing key secret
resource "aws_secretsmanager_secret" "jwt_signing_key" {
  name_prefix             = "stockxpress/${var.environment}/jwt-signing-key-"
  description             = "JWT signing key for StockXpress ${var.environment}"
  recovery_window_in_days = 7

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-jwt-signing-key"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )
}

# Observability basic auth credentials
resource "aws_secretsmanager_secret" "observability_basic_auth" {
  name                    = "stockxpress/observability/basic-auth"
  description             = "Basic auth credentials for observability endpoints"
  recovery_window_in_days = 7

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-observability-basic-auth"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )
}

# SECURITY NOTE: Secret values must be populated separately using one of these methods:
# 1. AWS Secrets Manager Console
# 2. AWS CLI: aws secretsmanager put-secret-value --secret-id <id> --secret-string '<json>'
# 3. CI/CD pipeline with proper IAM permissions
# 4. Terraform data source reading from external secure source

# IAM policy for applications to read secrets
resource "aws_iam_policy" "secrets_read" {
  name_prefix = "stockxpress-${var.environment}-secrets-read-"
  description = "Allow reading StockXpress secrets from Secrets Manager"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = [
          aws_secretsmanager_secret.db_credentials.arn,
          aws_secretsmanager_secret.redis_auth_token.arn,
          aws_secretsmanager_secret.api_keys.arn,
          aws_secretsmanager_secret.jwt_signing_key.arn,
          aws_secretsmanager_secret.observability_basic_auth.arn
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt"
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "kms:ViaService" : "secretsmanager.${data.aws_region.current.name}.amazonaws.com"
          }
        }
      }
    ]
  })

  tags = merge(
    var.tags,
    {
      Name        = "stockxpress-${var.environment}-secrets-read-policy"
      Environment = var.environment
    }
  )
}

# Data source for current region
data "aws_region" "current" {}

# Outputs - ARNs only, never expose secret values
output "db_credentials_secret_arn" {
  description = "ARN of the database credentials secret"
  value       = aws_secretsmanager_secret.db_credentials.arn
}

output "redis_auth_token_secret_arn" {
  description = "ARN of the Redis auth token secret"
  value       = aws_secretsmanager_secret.redis_auth_token.arn
}

output "api_keys_secret_arn" {
  description = "ARN of the API keys secret"
  value       = aws_secretsmanager_secret.api_keys.arn
}

output "jwt_signing_key_secret_arn" {
  description = "ARN of the JWT signing key secret"
  value       = aws_secretsmanager_secret.jwt_signing_key.arn
}

output "observability_basic_auth_secret_arn" {
  description = "ARN of the observability basic auth secret"
  value       = aws_secretsmanager_secret.observability_basic_auth.arn
}

output "secrets_read_policy_arn" {
  description = "ARN of the IAM policy for reading secrets"
  value       = aws_iam_policy.secrets_read.arn
}

output "db_credentials_secret_name" {
  description = "Name of the database credentials secret"
  value       = aws_secretsmanager_secret.db_credentials.name
}

output "redis_auth_token_secret_name" {
  description = "Name of the Redis auth token secret"
  value       = aws_secretsmanager_secret.redis_auth_token.name
}
