# ECR Module Outputs

output "repository_urls" {
  description = "Map of repository names to URLs"
  value       = { for k, v in aws_ecr_repository.services : k => v.repository_url }
}

output "repository_arns" {
  description = "Map of repository names to ARNs"
  value       = { for k, v in aws_ecr_repository.services : k => v.arn }
}

output "repository_registry_ids" {
  description = "Map of repository names to registry IDs"
  value       = { for k, v in aws_ecr_repository.services : k => v.registry_id }
}

output "kms_key_id" {
  description = "KMS key ID used for ECR encryption"
  value       = var.create_kms_key ? aws_kms_key.ecr[0].id : var.kms_key_arn
}

output "kms_key_arn" {
  description = "KMS key ARN used for ECR encryption"
  value       = var.create_kms_key ? aws_kms_key.ecr[0].arn : var.kms_key_arn
}

output "registry_id" {
  description = "Registry ID (AWS account ID)"
  value       = data.aws_caller_identity.current.account_id
}

output "docker_login_command" {
  description = "Docker login command for ECR"
  value       = "aws ecr get-login-password --region ${data.aws_region.current.name} | docker login --username AWS --password-stdin ${data.aws_caller_identity.current.account_id}.dkr.ecr.${data.aws_region.current.name}.amazonaws.com"
}

# Data source for region
data "aws_region" "current" {}
