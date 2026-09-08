# Secrets Manager Module Outputs

output "rds_order_secret_arn" {
  description = "ARN of RDS Order database secret"
  value       = aws_secretsmanager_secret.rds_order.arn
}

output "rds_order_secret_name" {
  description = "Name of RDS Order database secret"
  value       = aws_secretsmanager_secret.rds_order.name
}

output "rds_inventory_secret_arn" {
  description = "ARN of RDS Inventory database secret"
  value       = aws_secretsmanager_secret.rds_inventory.arn
}

output "rds_inventory_secret_name" {
  description = "Name of RDS Inventory database secret"
  value       = aws_secretsmanager_secret.rds_inventory.name
}

output "documentdb_secret_arn" {
  description = "ARN of DocumentDB secret"
  value       = aws_secretsmanager_secret.documentdb.arn
}

output "documentdb_secret_name" {
  description = "Name of DocumentDB secret"
  value       = aws_secretsmanager_secret.documentdb.name
}

output "elasticache_secret_arn" {
  description = "ARN of ElastiCache secret"
  value       = aws_secretsmanager_secret.elasticache.arn
}

output "elasticache_secret_name" {
  description = "Name of ElastiCache secret"
  value       = aws_secretsmanager_secret.elasticache.name
}

output "jwt_secret_arn" {
  description = "ARN of JWT secret"
  value       = var.create_jwt_secret ? aws_secretsmanager_secret.jwt[0].arn : null
}

output "jwt_secret_name" {
  description = "Name of JWT secret"
  value       = var.create_jwt_secret ? aws_secretsmanager_secret.jwt[0].name : null
}

output "keycloak_secret_arn" {
  description = "ARN of Keycloak secret"
  value       = var.create_keycloak_secret ? aws_secretsmanager_secret.keycloak[0].arn : null
}

output "keycloak_secret_name" {
  description = "Name of Keycloak secret"
  value       = var.create_keycloak_secret ? aws_secretsmanager_secret.keycloak[0].name : null
}

output "email_secret_arn" {
  description = "ARN of email SMTP secret"
  value       = var.create_email_secret ? aws_secretsmanager_secret.email[0].arn : null
}

output "email_secret_name" {
  description = "Name of email SMTP secret"
  value       = var.create_email_secret ? aws_secretsmanager_secret.email[0].name : null
}

output "kms_key_id" {
  description = "KMS key ID used for secrets encryption"
  value       = var.create_kms_key ? aws_kms_key.secrets[0].id : var.kms_key_id
}

output "kms_key_arn" {
  description = "KMS key ARN used for secrets encryption"
  value       = var.create_kms_key ? aws_kms_key.secrets[0].arn : var.kms_key_id
}

output "all_secret_arns" {
  description = "Map of all secret ARNs"
  value = {
    rds_order     = aws_secretsmanager_secret.rds_order.arn
    rds_inventory = aws_secretsmanager_secret.rds_inventory.arn
    documentdb    = aws_secretsmanager_secret.documentdb.arn
    elasticache   = aws_secretsmanager_secret.elasticache.arn
    jwt           = var.create_jwt_secret ? aws_secretsmanager_secret.jwt[0].arn : null
    keycloak      = var.create_keycloak_secret ? aws_secretsmanager_secret.keycloak[0].arn : null
    email         = var.create_email_secret ? aws_secretsmanager_secret.email[0].arn : null
  }
}
