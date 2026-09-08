# RDS Module - Outputs

output "db_instance_ids" {
  description = "Map of database instance identifiers"
  value       = { for k, v in aws_db_instance.main : k => v.id }
}

output "db_instance_arns" {
  description = "Map of database instance ARNs"
  value       = { for k, v in aws_db_instance.main : k => v.arn }
}

output "db_instance_endpoints" {
  description = "Map of database instance endpoints"
  value       = { for k, v in aws_db_instance.main : k => v.endpoint }
}

output "db_instance_addresses" {
  description = "Map of database instance addresses"
  value       = { for k, v in aws_db_instance.main : k => v.address }
}

output "db_instance_ports" {
  description = "Map of database instance ports"
  value       = { for k, v in aws_db_instance.main : k => v.port }
}

output "db_instance_names" {
  description = "Map of database names"
  value       = { for k, v in aws_db_instance.main : k => v.db_name }
}

output "db_instance_usernames" {
  description = "Map of master usernames"
  value       = { for k, v in aws_db_instance.main : k => v.username }
  sensitive   = true
}

output "db_instance_passwords" {
  description = "Map of master passwords"
  value       = { for k, v in random_password.master : k => v.result }
  sensitive   = true
}

output "db_instance_resource_ids" {
  description = "Map of RDS Resource IDs"
  value       = { for k, v in aws_db_instance.main : k => v.resource_id }
}

output "db_instance_status" {
  description = "Map of database instance status"
  value       = { for k, v in aws_db_instance.main : k => v.status }
}

output "db_instance_availability_zones" {
  description = "Map of availability zones"
  value       = { for k, v in aws_db_instance.main : k => v.availability_zone }
}

output "db_instance_multi_az" {
  description = "Map of Multi-AZ status"
  value       = { for k, v in aws_db_instance.main : k => v.multi_az }
}

output "db_security_group_ids" {
  description = "Map of security group IDs"
  value       = { for k, v in aws_security_group.rds : k => v.id }
}

output "db_parameter_group_ids" {
  description = "Map of parameter group IDs"
  value       = { for k, v in aws_db_parameter_group.main : k => v.id }
}

output "db_parameter_group_names" {
  description = "Map of parameter group names"
  value       = { for k, v in aws_db_parameter_group.main : k => v.name }
}

output "db_option_group_ids" {
  description = "Map of option group IDs"
  value       = { for k, v in aws_db_option_group.main : k => v.id }
}

output "db_option_group_names" {
  description = "Map of option group names"
  value       = { for k, v in aws_db_option_group.main : k => v.name }
}

output "kms_key_ids" {
  description = "Map of KMS key IDs used for encryption"
  value       = { for k, v in aws_kms_key.rds : k => v.key_id }
}

output "kms_key_arns" {
  description = "Map of KMS key ARNs used for encryption"
  value       = { for k, v in aws_kms_key.rds : k => v.arn }
}

output "secrets_manager_secret_ids" {
  description = "Map of Secrets Manager secret IDs"
  value       = var.store_master_password_in_secrets_manager ? { for k, v in aws_secretsmanager_secret.db_password : k => v.id } : {}
}

output "secrets_manager_secret_arns" {
  description = "Map of Secrets Manager secret ARNs"
  value       = var.store_master_password_in_secrets_manager ? { for k, v in aws_secretsmanager_secret.db_password : k => v.arn } : {}
}

output "monitoring_role_arn" {
  description = "ARN of the IAM role for enhanced monitoring"
  value       = var.default_monitoring_interval > 0 ? aws_iam_role.rds_monitoring[0].arn : null
}

output "cloudwatch_alarm_ids" {
  description = "Map of CloudWatch alarm IDs"
  value = var.enable_cloudwatch_alarms ? {
    cpu     = { for k, v in aws_cloudwatch_metric_alarm.cpu : k => v.id }
    storage = { for k, v in aws_cloudwatch_metric_alarm.storage : k => v.id }
    memory  = { for k, v in aws_cloudwatch_metric_alarm.memory : k => v.id }
  } : {}
}

output "connection_strings" {
  description = "Map of database connection strings"
  value = {
    for k, v in aws_db_instance.main : k => "mysql://${v.username}:${random_password.master[k].result}@${v.endpoint}/${v.db_name}"
  }
  sensitive = true
}
