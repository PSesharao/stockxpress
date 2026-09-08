# AWS Secrets Manager Module
# Manages database credentials and other sensitive data

# KMS key for Secrets Manager encryption
resource "aws_kms_key" "secrets" {
  count               = var.create_kms_key ? 1 : 0
  description         = "KMS key for Secrets Manager encryption"
  enable_key_rotation = true

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-secrets-kms"
    }
  )
}

resource "aws_kms_alias" "secrets" {
  count         = var.create_kms_key ? 1 : 0
  name          = "alias/${var.project_name}-${var.environment}-secrets"
  target_key_id = aws_kms_key.secrets[0].key_id
}

# RDS Order Database Password
resource "aws_secretsmanager_secret" "rds_order" {
  name_prefix             = "${var.project_name}/${var.environment}/rds/order-"
  description             = "Order Service RDS database credentials"
  kms_key_id              = var.create_kms_key ? aws_kms_key.secrets[0].id : var.kms_key_id
  recovery_window_in_days = var.recovery_window_in_days

  tags = merge(
    var.tags,
    {
      Name        = "${var.project_name}-${var.environment}-rds-order"
      Service     = "order-service"
      SecretType  = "database"
    }
  )
}

resource "aws_secretsmanager_secret_version" "rds_order" {
  secret_id = aws_secretsmanager_secret.rds_order.id
  secret_string = jsonencode({
    username = var.rds_order_username
    password = var.rds_order_password
    engine   = "mysql"
    host     = var.rds_order_endpoint
    port     = var.rds_order_port
    dbname   = var.rds_order_database
  })
}

# RDS Inventory Database Password
resource "aws_secretsmanager_secret" "rds_inventory" {
  name_prefix             = "${var.project_name}/${var.environment}/rds/inventory-"
  description             = "Inventory Service RDS database credentials"
  kms_key_id              = var.create_kms_key ? aws_kms_key.secrets[0].id : var.kms_key_id
  recovery_window_in_days = var.recovery_window_in_days

  tags = merge(
    var.tags,
    {
      Name        = "${var.project_name}-${var.environment}-rds-inventory"
      Service     = "inventory-service"
      SecretType  = "database"
    }
  )
}

resource "aws_secretsmanager_secret_version" "rds_inventory" {
  secret_id = aws_secretsmanager_secret.rds_inventory.id
  secret_string = jsonencode({
    username = var.rds_inventory_username
    password = var.rds_inventory_password
    engine   = "mysql"
    host     = var.rds_inventory_endpoint
    port     = var.rds_inventory_port
    dbname   = var.rds_inventory_database
  })
}

# DocumentDB Password
resource "aws_secretsmanager_secret" "documentdb" {
  name_prefix             = "${var.project_name}/${var.environment}/documentdb/product-"
  description             = "Product Service DocumentDB credentials"
  kms_key_id              = var.create_kms_key ? aws_kms_key.secrets[0].id : var.kms_key_id
  recovery_window_in_days = var.recovery_window_in_days

  tags = merge(
    var.tags,
    {
      Name        = "${var.project_name}-${var.environment}-documentdb"
      Service     = "product-service"
      SecretType  = "database"
    }
  )
}

resource "aws_secretsmanager_secret_version" "documentdb" {
  secret_id = aws_secretsmanager_secret.documentdb.id
  secret_string = jsonencode({
    username = var.documentdb_username
    password = var.documentdb_password
    engine   = "docdb"
    host     = var.documentdb_endpoint
    port     = var.documentdb_port
    dbname   = var.documentdb_database
  })
}

# ElastiCache Redis Auth Token
resource "aws_secretsmanager_secret" "elasticache" {
  name_prefix             = "${var.project_name}/${var.environment}/elasticache/redis-"
  description             = "ElastiCache Redis authentication token"
  kms_key_id              = var.create_kms_key ? aws_kms_key.secrets[0].id : var.kms_key_id
  recovery_window_in_days = var.recovery_window_in_days

  tags = merge(
    var.tags,
    {
      Name        = "${var.project_name}-${var.environment}-elasticache"
      Service     = "inventory-service"
      SecretType  = "cache"
    }
  )
}

resource "aws_secretsmanager_secret_version" "elasticache" {
  secret_id = aws_secretsmanager_secret.elasticache.id
  secret_string = jsonencode({
    auth_token = var.elasticache_auth_token
    endpoint   = var.elasticache_endpoint
    port       = var.elasticache_port
  })
}

# JWT Secret for API Gateway
resource "random_password" "jwt_secret" {
  count   = var.create_jwt_secret ? 1 : 0
  length  = 64
  special = true
}

resource "aws_secretsmanager_secret" "jwt" {
  count                   = var.create_jwt_secret ? 1 : 0
  name_prefix             = "${var.project_name}/${var.environment}/jwt/secret-"
  description             = "JWT signing secret for API Gateway"
  kms_key_id              = var.create_kms_key ? aws_kms_key.secrets[0].id : var.kms_key_id
  recovery_window_in_days = var.recovery_window_in_days

  tags = merge(
    var.tags,
    {
      Name        = "${var.project_name}-${var.environment}-jwt"
      Service     = "api-gateway"
      SecretType  = "authentication"
    }
  )
}

resource "aws_secretsmanager_secret_version" "jwt" {
  count     = var.create_jwt_secret ? 1 : 0
  secret_id = aws_secretsmanager_secret.jwt[0].id
  secret_string = jsonencode({
    secret = random_password.jwt_secret[0].result
  })
}

# Keycloak Admin Credentials
resource "random_password" "keycloak_admin" {
  count   = var.create_keycloak_secret ? 1 : 0
  length  = 32
  special = true
}

resource "aws_secretsmanager_secret" "keycloak" {
  count                   = var.create_keycloak_secret ? 1 : 0
  name_prefix             = "${var.project_name}/${var.environment}/keycloak/admin-"
  description             = "Keycloak admin credentials"
  kms_key_id              = var.create_kms_key ? aws_kms_key.secrets[0].id : var.kms_key_id
  recovery_window_in_days = var.recovery_window_in_days

  tags = merge(
    var.tags,
    {
      Name        = "${var.project_name}-${var.environment}-keycloak"
      Service     = "keycloak"
      SecretType  = "admin"
    }
  )
}

resource "aws_secretsmanager_secret_version" "keycloak" {
  count     = var.create_keycloak_secret ? 1 : 0
  secret_id = aws_secretsmanager_secret.keycloak[0].id
  secret_string = jsonencode({
    username = var.keycloak_admin_username
    password = random_password.keycloak_admin[0].result
  })
}

# Email Service Credentials (for notifications)
resource "aws_secretsmanager_secret" "email" {
  count                   = var.create_email_secret ? 1 : 0
  name_prefix             = "${var.project_name}/${var.environment}/email/smtp-"
  description             = "SMTP credentials for email notifications"
  kms_key_id              = var.create_kms_key ? aws_kms_key.secrets[0].id : var.kms_key_id
  recovery_window_in_days = var.recovery_window_in_days

  tags = merge(
    var.tags,
    {
      Name        = "${var.project_name}-${var.environment}-email"
      Service     = "notification-service"
      SecretType  = "smtp"
    }
  )
}

resource "aws_secretsmanager_secret_version" "email" {
  count     = var.create_email_secret ? 1 : 0
  secret_id = aws_secretsmanager_secret.email[0].id
  secret_string = jsonencode({
    host     = var.smtp_host
    port     = var.smtp_port
    username = var.smtp_username
    password = var.smtp_password
  })
}

# Rotation Lambda IAM Role (for automatic password rotation)
resource "aws_iam_role" "secrets_rotation" {
  count = var.enable_rotation ? 1 : 0
  name  = "${var.project_name}-${var.environment}-secrets-rotation"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = var.tags
}

resource "aws_iam_role_policy" "secrets_rotation" {
  count = var.enable_rotation ? 1 : 0
  name  = "secrets-rotation-policy"
  role  = aws_iam_role.secrets_rotation[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:DescribeSecret",
          "secretsmanager:GetSecretValue",
          "secretsmanager:PutSecretValue",
          "secretsmanager:UpdateSecretVersionStage"
        ]
        Resource = "arn:aws:secretsmanager:*:*:secret:${var.project_name}/${var.environment}/*"
      },
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetRandomPassword"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:DescribeKey",
          "kms:GenerateDataKey"
        ]
        Resource = var.create_kms_key ? aws_kms_key.secrets[0].arn : var.kms_key_id
      }
    ]
  })
}

# Resource policy for Secrets Manager (allows specific services to read)
resource "aws_secretsmanager_secret_policy" "rds_order" {
  count      = var.enable_resource_policy ? 1 : 0
  secret_arn = aws_secretsmanager_secret.rds_order.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowEKSServiceAccountAccess"
        Effect = "Allow"
        Principal = {
          AWS = var.eks_service_account_roles
        }
        Action   = "secretsmanager:GetSecretValue"
        Resource = "*"
      }
    ]
  })
}
