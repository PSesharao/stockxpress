# MSK Module Outputs

output "cluster_arn" {
  description = "ARN of the MSK cluster"
  value       = aws_msk_cluster.kafka.arn
}

output "cluster_name" {
  description = "Name of the MSK cluster"
  value       = aws_msk_cluster.kafka.cluster_name
}

output "bootstrap_brokers" {
  description = "Plaintext bootstrap brokers"
  value       = aws_msk_cluster.kafka.bootstrap_brokers
}

output "bootstrap_brokers_tls" {
  description = "TLS bootstrap brokers"
  value       = aws_msk_cluster.kafka.bootstrap_brokers_tls
}

output "bootstrap_brokers_sasl_scram" {
  description = "SASL/SCRAM bootstrap brokers"
  value       = var.enable_scram_authentication ? aws_msk_cluster.kafka.bootstrap_brokers_sasl_scram : null
}

output "bootstrap_brokers_sasl_iam" {
  description = "SASL/IAM bootstrap brokers"
  value       = var.enable_iam_authentication ? aws_msk_cluster.kafka.bootstrap_brokers_sasl_iam : null
}

output "zookeeper_connect_string" {
  description = "Zookeeper connection string"
  value       = aws_msk_cluster.kafka.zookeeper_connect_string
}

output "security_group_id" {
  description = "Security group ID for MSK cluster"
  value       = aws_security_group.msk.id
}

output "configuration_arn" {
  description = "ARN of MSK configuration"
  value       = aws_msk_configuration.kafka.arn
}

output "configuration_revision" {
  description = "Latest revision of MSK configuration"
  value       = aws_msk_configuration.kafka.latest_revision
}

output "kms_key_id" {
  description = "KMS key ID used for encryption"
  value       = var.create_kms_key ? aws_kms_key.msk[0].id : var.kms_key_arn
}

output "scram_secret_arn" {
  description = "ARN of SCRAM secret in Secrets Manager"
  value       = var.enable_scram_authentication ? aws_secretsmanager_secret.msk_scram[0].arn : null
  sensitive   = true
}

output "scram_username" {
  description = "SCRAM username"
  value       = var.enable_scram_authentication ? var.scram_username : null
  sensitive   = true
}

# SECURITY: Password output removed - passwords must never be exposed in Terraform outputs
# Retrieve SCRAM password from AWS Secrets Manager instead:
#   aws secretsmanager get-secret-value --secret-id "AmazonMSK_stockxpress_ENV" --query SecretString --output text | jq -r '.password'

output "cloudwatch_log_group_name" {
  description = "CloudWatch log group name for MSK"
  value       = aws_cloudwatch_log_group.msk.name
}

output "s3_logs_bucket" {
  description = "S3 bucket for MSK logs"
  value       = var.enable_s3_logs ? aws_s3_bucket.msk_logs[0].id : null
}
