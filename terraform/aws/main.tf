# StockXpress AWS Infrastructure - Main Configuration
# Provisions EKS cluster, VPC, RDS instances, ElastiCache, MSK (Kafka), and supporting resources

terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  
  # Remote state storage (uncomment for production)
  # backend "s3" {
  #   bucket         = "stockxpress-terraform-state"
  #   key            = "eks/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "stockxpress-terraform-locks"
  # }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "StockXpress"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Owner       = var.owner
      CostCenter  = var.cost_center
    }
  }
}

provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
  
  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args = [
      "eks",
      "get-token",
      "--cluster-name",
      module.eks.cluster_name,
      "--region",
      var.aws_region
    ]
  }
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
    
    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args = [
        "eks",
        "get-token",
        "--cluster-name",
        module.eks.cluster_name,
        "--region",
        var.aws_region
      ]
    }
  }
}

# Local variables
locals {
  cluster_name = "${var.project_name}-${var.environment}-eks"
  
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# VPC Module
module "vpc" {
  source = "./modules/vpc"
  
  project_name        = var.project_name
  environment         = var.environment
  vpc_cidr            = var.vpc_cidr
  availability_zones  = var.availability_zones
  public_subnet_cidrs = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  
  tags = local.common_tags
}

# EKS Cluster Module
module "eks" {
  source = "./modules/eks"
  
  cluster_name       = local.cluster_name
  cluster_version    = var.eks_cluster_version
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  
  node_groups = var.eks_node_groups
  
  tags = local.common_tags
}

# RDS Module (Order and Inventory databases)
module "rds" {
  source = "./modules/rds"
  
  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.vpc.vpc_id
  database_subnet_ids = module.vpc.database_subnet_ids
  
  # Order Service Database
  order_db_config = {
    instance_class    = var.rds_instance_class
    allocated_storage = var.rds_allocated_storage
    engine_version    = var.mysql_engine_version
    database_name     = "order_service"
    username          = "orderadmin"
    multi_az          = var.environment == "production" ? true : false
  }
  
  # Inventory Service Database
  inventory_db_config = {
    instance_class    = var.rds_instance_class
    allocated_storage = var.rds_allocated_storage
    engine_version    = var.mysql_engine_version
    database_name     = "inventory_service"
    username          = "inventoryadmin"
    multi_az          = var.environment == "production" ? true : false
  }
  
  tags = local.common_tags
}

# DocumentDB Module (Product Service - MongoDB compatible)
module "documentdb" {
  source = "./modules/documentdb"
  
  project_name        = var.project_name
  environment         = var.environment
  vpc_id              = module.vpc.vpc_id
  database_subnet_ids = module.vpc.database_subnet_ids
  
  cluster_size       = var.environment == "production" ? 3 : 1
  instance_class     = var.documentdb_instance_class
  master_username    = "productadmin"
  
  tags = local.common_tags
}

# ElastiCache Redis Module (Inventory Service caching)
module "elasticache" {
  source = "./modules/elasticache"
  
  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.vpc.vpc_id
  cache_subnet_ids   = module.vpc.private_subnet_ids
  
  node_type          = var.redis_node_type
  num_cache_nodes    = var.environment == "production" ? 2 : 1
  engine_version     = var.redis_engine_version
  
  tags = local.common_tags
}

# MSK (Managed Streaming for Kafka) Module
module "msk" {
  source = "./modules/msk"
  
  project_name    = var.project_name
  environment     = var.environment
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids
  
  kafka_version   = var.kafka_version
  broker_instance_type = var.kafka_broker_instance_type
  number_of_broker_nodes = length(var.availability_zones)
  
  tags = local.common_tags
}

# ECR Repositories
module "ecr" {
  source = "./modules/ecr"
  
  project_name = var.project_name
  environment  = var.environment
  
  repositories = [
    "discovery-server",
    "api-gateway",
    "product-service",
    "inventory-service",
    "order-service",
    "notification-service"
  ]
  
  tags = local.common_tags
}

# Secrets Manager for sensitive data
module "secrets_manager" {
  source = "./modules/secrets"
  
  project_name = var.project_name
  environment  = var.environment
  
  # Database passwords
  rds_order_password      = module.rds.order_db_password
  rds_inventory_password  = module.rds.inventory_db_password
  documentdb_password     = module.documentdb.master_password
  elasticache_auth_token  = module.elasticache.auth_token
  
  tags = local.common_tags
}

# Kubernetes Addons
module "k8s_addons" {
  source = "./modules/k8s-addons"
  
  depends_on = [module.eks]
  
  cluster_name    = module.eks.cluster_name
  cluster_version = var.eks_cluster_version
  
  # Enable addons
  enable_metrics_server     = true
  enable_cluster_autoscaler = true
  enable_aws_load_balancer_controller = true
  enable_external_secrets   = true
  enable_prometheus         = true
  enable_grafana            = true
  
  tags = local.common_tags
}