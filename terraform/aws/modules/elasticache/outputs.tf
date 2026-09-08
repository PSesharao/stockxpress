# ElastiCache Redis Module Outputs

output "replication_group_id" {
  description = "ID of the ElastiCache replication group"
  value       = aws_elasticache_replication_group.redis.id
}

output "replication_group_arn" {
  description = "ARN of the ElastiCache replication group"
  value       = aws_elasticache_replication_group.redis.arn
}

output "primary_endpoint_address" {
  description = "Primary endpoint address for the replication group"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "reader_endpoint_address" {
  description = "Reader endpoint address for the replication group"
  value       = aws_elasticache_replication_group.redis.reader_endpoint_address
}

output "configuration_endpoint_address" {
  description = "Configuration endpoint address (cluster mode only)"
  value       = var.cluster_mode_enabled ? aws_elasticache_replication_group.redis.configuration_endpoint_address : null
}

output "port" {
  description = "Port number for Redis"
  value       = aws_elasticache_replication_group.redis.port
}

# SECURITY: Auth token output removed - tokens must never be exposed in Terraform outputs
# Retrieve auth token from AWS Secrets Manager instead:
#   aws secretsmanager get-secret-value --secret-id "stockxpress/ENV/redis-auth-token" --query SecretString --output text

output "security_group_id" {
  description = "Security group ID for Redis"
  value       = aws_security_group.redis.id
}

output "subnet_group_name" {
  description = "ElastiCache subnet group name"
  value       = aws_elasticache_subnet_group.redis.name
}

output "parameter_group_name" {
  description = "ElastiCache parameter group name"
  value       = aws_elasticache_parameter_group.redis.name
}

output "member_clusters" {
  description = "List of member cluster IDs"
  value       = aws_elasticache_replication_group.redis.member_clusters
}

output "cluster_enabled" {
  description = "Whether cluster mode is enabled"
  value       = aws_elasticache_replication_group.redis.cluster_enabled
}

# SECURITY: Connection string output removed - contains embedded auth tokens
# Build connection strings in your application using:
#   - Endpoint: from primary_endpoint_address output
#   - Port: from port output
#   - Auth Token: from AWS Secrets Manager
#   - TLS: based on transit_encryption_enabled setting
# Example: rediss://:${auth_token_from_secrets_manager}@${endpoint}:${port}
