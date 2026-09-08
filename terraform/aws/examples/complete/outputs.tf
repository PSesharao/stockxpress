# Complete Example - Outputs

# VPC Outputs
output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "vpc_cidr" {
  description = "VPC CIDR block"
  value       = module.vpc.vpc_cidr
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = module.vpc.private_subnet_ids
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = module.vpc.public_subnet_ids
}

output "database_subnet_ids" {
  description = "Database subnet IDs"
  value       = module.vpc.database_subnet_ids
}

output "nat_gateway_ips" {
  description = "NAT Gateway public IPs"
  value       = module.vpc.nat_public_ips
}

# EKS Outputs
output "eks_cluster_id" {
  description = "EKS cluster ID"
  value       = module.eks.cluster_id
}

output "eks_cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = module.eks.cluster_endpoint
}

output "eks_cluster_version" {
  description = "EKS cluster Kubernetes version"
  value       = module.eks.cluster_version
}

output "eks_cluster_security_group_id" {
  description = "EKS cluster security group ID"
  value       = module.eks.cluster_security_group_id
}

output "eks_node_security_group_id" {
  description = "EKS node security group ID"
  value       = module.eks.node_security_group_id
}

output "eks_oidc_provider_arn" {
  description = "OIDC provider ARN for IRSA"
  value       = module.eks.oidc_provider_arn
}

output "eks_oidc_provider_url" {
  description = "OIDC provider URL"
  value       = module.eks.oidc_provider_url
}

output "eks_node_groups" {
  description = "EKS node group details"
  value       = module.eks.node_groups
}

# RDS Outputs
output "rds_order_endpoint" {
  description = "Order database endpoint"
  value       = module.rds.db_instance_endpoints["order"]
}

output "rds_inventory_endpoint" {
  description = "Inventory database endpoint"
  value       = module.rds.db_instance_endpoints["inventory"]
}

output "rds_order_secret_arn" {
  description = "Order database secret ARN"
  value       = lookup(module.rds.secrets_manager_secret_arns, "order", "")
}

output "rds_inventory_secret_arn" {
  description = "Inventory database secret ARN"
  value       = lookup(module.rds.secrets_manager_secret_arns, "inventory", "")
}

output "rds_security_group_ids" {
  description = "RDS security group IDs"
  value       = module.rds.db_security_group_ids
}

# Kubernetes Configuration
output "configure_kubectl" {
  description = "Command to configure kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${local.cluster_name}"
}

# Connection Information
output "database_connection_info" {
  description = "Database connection information"
  value = {
    order = {
      endpoint   = module.rds.db_instance_endpoints["order"]
      secret_arn = lookup(module.rds.secrets_manager_secret_arns, "order", "")
      retrieve_credentials = "aws secretsmanager get-secret-value --secret-id ${lookup(module.rds.secrets_manager_secret_arns, "order", "")} --query SecretString --output text | jq ."
    }
    inventory = {
      endpoint   = module.rds.db_instance_endpoints["inventory"]
      secret_arn = lookup(module.rds.secrets_manager_secret_arns, "inventory", "")
      retrieve_credentials = "aws secretsmanager get-secret-value --secret-id ${lookup(module.rds.secrets_manager_secret_arns, "inventory", "")} --query SecretString --output text | jq ."
    }
  }
}
