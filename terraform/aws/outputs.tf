# StockXpress AWS Infrastructure - Outputs

# VPC Outputs
output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "private_subnet_ids" {
  description = "List of private subnet IDs"
  value       = module.vpc.private_subnet_ids
}

output "public_subnet_ids" {
  description = "List of public subnet IDs"
  value       = module.vpc.public_subnet_ids
}

# EKS Outputs
output "eks_cluster_name" {
  description = "Name of the EKS cluster"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "Endpoint for EKS cluster"
  value       = module.eks.cluster_endpoint
}

output "eks_cluster_security_group_id" {
  description = "Security group ID attached to the EKS cluster"
  value       = module.eks.cluster_security_group_id
}

output "eks_cluster_oidc_issuer_url" {
  description = "OIDC issuer URL for the EKS cluster"
  value       = module.eks.cluster_oidc_issuer_url
}

output "configure_kubectl" {
  description = "Command to configure kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

# RDS Outputs
output "rds_order_endpoint" {
  description = "Endpoint for Order Service RDS instance"
  value       = module.rds.order_db_endpoint
  sensitive   = true
}

output "rds_inventory_endpoint" {
  description = "Endpoint for Inventory Service RDS instance"
  value       = module.rds.inventory_db_endpoint
  sensitive   = true
}

# DocumentDB Outputs
output "documentdb_endpoint" {
  description = "Endpoint for DocumentDB cluster"
  value       = module.documentdb.cluster_endpoint
  sensitive   = true
}

output "documentdb_reader_endpoint" {
  description = "Reader endpoint for DocumentDB cluster"
  value       = module.documentdb.cluster_reader_endpoint
  sensitive   = true
}

# ElastiCache Outputs
output "redis_endpoint" {
  description = "Endpoint for ElastiCache Redis"
  value       = module.elasticache.redis_endpoint
  sensitive   = true
}

output "redis_configuration_endpoint" {
  description = "Configuration endpoint for ElastiCache Redis"
  value       = module.elasticache.redis_configuration_endpoint
  sensitive   = true
}

# MSK Outputs
output "msk_bootstrap_brokers" {
  description = "MSK cluster bootstrap brokers"
  value       = module.msk.bootstrap_brokers
  sensitive   = true
}

output "msk_zookeeper_connect_string" {
  description = "MSK cluster Zookeeper connection string"
  value       = module.msk.zookeeper_connect_string
  sensitive   = true
}

# ECR Outputs
output "ecr_repository_urls" {
  description = "Map of ECR repository URLs"
  value       = module.ecr.repository_urls
}

# Secrets Manager Outputs
output "secrets_manager_arns" {
  description = "ARNs of Secrets Manager secrets"
  value       = module.secrets_manager.secret_arns
  sensitive   = true
}

# Quick Start Commands
output "quick_start_commands" {
  description = "Commands to get started with the infrastructure"
  value = <<-EOT
    # Configure kubectl
    aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}
    
    # Verify cluster access
    kubectl get nodes
    
    # Create namespace
    kubectl create namespace stockxpress
    
    # Install External Secrets Operator (if enabled)
    helm repo add external-secrets https://charts.external-secrets.io
    helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace
    
    # Push Docker images to ECR
    aws ecr get-login-password --region ${var.aws_region} | docker login --username AWS --password-stdin ${split("/", module.ecr.repository_urls["api-gateway"])[0]}
    
    # Build and push (example for API Gateway)
    docker build -t ${module.ecr.repository_urls["api-gateway"]}:latest -f api-gateway/Dockerfile .
    docker push ${module.ecr.repository_urls["api-gateway"]}:latest
  EOT
}