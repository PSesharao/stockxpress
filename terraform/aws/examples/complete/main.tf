# Complete Example - VPC, EKS, and RDS
# This example demonstrates how to use all three modules together

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
  }

  # Uncomment for remote state
  # backend "s3" {
  #   bucket         = "my-terraform-state"
  #   key            = "aws/infrastructure/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "terraform-state-lock"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = var.common_tags
  }
}

data "aws_caller_identity" "current" {}
data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  cluster_name = "${var.project_name}-${var.environment}-eks"
  name_prefix  = "${var.project_name}-${var.environment}"
}

# VPC Module
module "vpc" {
  source = "../../modules/vpc"

  name         = local.name_prefix
  vpc_cidr     = var.vpc_cidr
  az_count     = var.az_count
  cluster_name = local.cluster_name

  enable_nat_gateway = var.enable_nat_gateway
  single_nat_gateway = var.single_nat_gateway
  enable_flow_logs   = var.enable_vpc_flow_logs

  flow_logs_retention_days = var.vpc_flow_logs_retention_days

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# EKS Module
module "eks" {
  source = "../../modules/eks"

  cluster_name    = local.cluster_name
  cluster_version = var.eks_cluster_version

  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  public_subnet_ids  = module.vpc.public_subnet_ids

  cluster_endpoint_private_access      = var.eks_endpoint_private_access
  cluster_endpoint_public_access       = var.eks_endpoint_public_access
  cluster_endpoint_public_access_cidrs = var.eks_endpoint_public_access_cidrs
  cluster_enabled_log_types            = var.eks_enabled_log_types

  node_groups = var.eks_node_groups

  enable_ebs_csi_driver = var.enable_ebs_csi_driver

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }

  depends_on = [module.vpc]
}

# RDS Module
module "rds" {
  source = "../../modules/rds"

  name_prefix          = local.name_prefix
  vpc_id               = module.vpc.vpc_id
  db_subnet_group_name = module.vpc.db_subnet_group_name

  databases = {
    order = {
      database_name              = "orders"
      instance_class             = var.rds_order_instance_class
      allocated_storage          = var.rds_order_allocated_storage
      max_allocated_storage      = var.rds_order_max_allocated_storage
      multi_az                   = var.rds_multi_az
      backup_retention_period    = var.rds_backup_retention_period
      deletion_protection        = var.rds_deletion_protection
      performance_insights_enabled = var.rds_performance_insights_enabled
      
      allowed_security_groups = [
        module.eks.node_security_group_id
      ]

      parameters = [
        {
          name  = "max_connections"
          value = "2000"
        },
        {
          name  = "character_set_server"
          value = "utf8mb4"
        },
        {
          name  = "collation_server"
          value = "utf8mb4_unicode_ci"
        },
      ]
    }

    inventory = {
      database_name              = "inventory"
      instance_class             = var.rds_inventory_instance_class
      allocated_storage          = var.rds_inventory_allocated_storage
      max_allocated_storage      = var.rds_inventory_max_allocated_storage
      multi_az                   = var.rds_multi_az
      backup_retention_period    = var.rds_backup_retention_period
      deletion_protection        = var.rds_deletion_protection
      performance_insights_enabled = var.rds_performance_insights_enabled
      
      allowed_security_groups = [
        module.eks.node_security_group_id
      ]

      parameters = [
        {
          name  = "max_connections"
          value = "1000"
        },
        {
          name  = "character_set_server"
          value = "utf8mb4"
        },
        {
          name  = "collation_server"
          value = "utf8mb4_unicode_ci"
        },
      ]
    }
  }

  enable_cloudwatch_alarms = var.enable_rds_cloudwatch_alarms
  alarm_actions            = var.rds_alarm_actions

  store_master_password_in_secrets_manager = var.store_db_passwords_in_secrets_manager

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }

  depends_on = [module.vpc]
}

# SNS Topic for Alarms (Optional)
resource "aws_sns_topic" "alerts" {
  count = var.create_sns_topic_for_alarms ? 1 : 0
  name  = "${local.name_prefix}-alerts"

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_sns_topic_subscription" "alerts_email" {
  count     = var.create_sns_topic_for_alarms && var.alert_email != "" ? 1 : 0
  topic_arn = aws_sns_topic.alerts[0].arn
  protocol  = "email"
  endpoint  = var.alert_email
}
