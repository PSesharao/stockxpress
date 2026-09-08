# StockXpress AWS Infrastructure - Terraform Documentation

[![Terraform](https://img.shields.io/badge/Terraform-v1.5+-623CE4?logo=terraform)](https://www.terraform.io/)
[![AWS](https://img.shields.io/badge/AWS-Cloud-FF9900?logo=amazon-aws)](https://aws.amazon.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-1.28-326CE5?logo=kubernetes)](https://kubernetes.io/)

Comprehensive Infrastructure-as-Code for deploying the StockXpress microservices platform on AWS using EKS, managed databases, and supporting services.

---

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [State Management Setup](#state-management-setup)
- [Environment-Specific Deployment](#environment-specific-deployment)
- [Module Documentation](#module-documentation)
- [Complete Deployment Workflow](#complete-deployment-workflow)
- [Cost Estimation](#cost-estimation)
- [Security Best Practices](#security-best-practices)
- [Maintenance & Operations](#maintenance--operations)
- [Troubleshooting Guide](#troubleshooting-guide)
- [FAQ](#faq)

---

## 🏗️ Architecture Overview

This Terraform configuration provisions a production-ready AWS infrastructure for the StockXpress microservices platform:

### Infrastructure Components

| Component | Service | Purpose |
|-----------|---------|----------|
| **Compute** | Amazon EKS 1.28 | Kubernetes cluster for microservices |
| **Networking** | VPC, Subnets, NAT Gateway | Isolated network infrastructure |
| **Databases** | RDS MySQL (x2) | Order & Inventory service data |
| **Document Store** | DocumentDB | Product service (MongoDB-compatible) |
| **Cache** | ElastiCache Redis | Inventory service caching |
| **Messaging** | MSK (Managed Kafka) | Event-driven communication |
| **Container Registry** | ECR | Docker image storage |
| **Secrets** | Secrets Manager | Secure credential storage |
| **Monitoring** | Prometheus, Grafana | Observability & metrics |
| **Add-ons** | Metrics Server, Cluster Autoscaler, ALB Controller, External Secrets | K8s operational tools |

### Network Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          VPC (10.0.0.0/16)                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │  Public Subnet   │  │  Public Subnet   │  │ Public Subnet│ │
│  │   10.0.1.0/24    │  │   10.0.2.0/24    │  │  10.0.3.0/24 │ │
│  │   (AZ-1)         │  │   (AZ-2)         │  │   (AZ-3)     │ │
│  │  - NAT Gateway   │  │  - NAT Gateway   │  │ - NAT Gateway│ │
│  │  - ALB           │  │  - ALB           │  │ - ALB        │ │
│  └──────────────────┘  └──────────────────┘  └──────────────┘ │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │ Private Subnet   │  │ Private Subnet   │  │Private Subnet│ │
│  │  10.0.11.0/24    │  │  10.0.12.0/24    │  │ 10.0.13.0/24 │ │
│  │   (AZ-1)         │  │   (AZ-2)         │  │   (AZ-3)     │ │
│  │  - EKS Nodes     │  │  - EKS Nodes     │  │ - EKS Nodes  │ │
│  │  - MSK Brokers   │  │  - MSK Brokers   │  │ - MSK Brokers│ │
│  │  - ElastiCache   │  │  - ElastiCache   │  │              │ │
│  └──────────────────┘  └──────────────────┘  └──────────────┘ │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │ Database Subnet  │  │ Database Subnet  │  │Database Subnet│
│  │  10.0.21.0/24    │  │  10.0.22.0/24    │  │ 10.0.23.0/24 │ │
│  │   (AZ-1)         │  │   (AZ-2)         │  │   (AZ-3)     │ │
│  │  - RDS (Order)   │  │  - RDS Standby   │  │              │ │
│  │  - RDS (Inv)     │  │  - DocumentDB    │  │              │ │
│  │  - DocumentDB    │  │                  │  │              │ │
│  └──────────────────┘  └──────────────────┘  └──────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📦 Prerequisites

### Required Tools

Ensure the following tools are installed and properly configured:

| Tool | Minimum Version | Installation |
|------|----------------|-------------|
| **Terraform** | 1.5.0+ | [Download](https://www.terraform.io/downloads) |
| **AWS CLI** | 2.0+ | [Install Guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) |
| **kubectl** | 1.28+ | [Install Guide](https://kubernetes.io/docs/tasks/tools/) |
| **helm** | 3.12+ | [Install Guide](https://helm.sh/docs/intro/install/) |
| **Docker** | 20.10+ | [Install Guide](https://docs.docker.com/get-docker/) |
| **jq** | 1.6+ | `brew install jq` / `apt-get install jq` |

### Verification Commands

```bash
# Verify tool installations
terraform version
aws --version
kubectl version --client
helm version
docker --version
jq --version
```

### AWS Requirements

#### 1. AWS Account Setup

- Active AWS account with appropriate permissions
- AWS CLI configured with credentials
- IAM user/role with sufficient permissions (see [IAM Policy](#iam-policy-requirements))

#### 2. Configure AWS CLI

```bash
# Configure AWS credentials
aws configure

# Verify access
aws sts get-caller-identity

# Output should show:
# {
#     "UserId": "AIDAXXXXXXXXXXXXXXXXX",
#     "Account": "123456789012",
#     "Arn": "arn:aws:iam::123456789012:user/your-username"
# }
```

#### 3. IAM Policy Requirements

The deploying user/role needs permissions for:

- **EC2**: VPC, Subnets, Security Groups, NAT Gateways
- **EKS**: Cluster creation and management
- **RDS**: Database instance creation
- **DocumentDB**: Cluster creation
- **ElastiCache**: Redis cluster creation
- **MSK**: Kafka cluster creation
- **ECR**: Repository creation and management
- **Secrets Manager**: Secret creation and retrieval
- **IAM**: Role and policy creation for EKS
- **CloudWatch**: Log groups and metrics
- **S3**: For Terraform state storage
- **DynamoDB**: For state locking

<details>
<summary>Click to view comprehensive IAM policy</summary>

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:*",
        "eks:*",
        "rds:*",
        "docdb:*",
        "elasticache:*",
        "kafka:*",
        "ecr:*",
        "secretsmanager:*",
        "iam:CreateRole",
        "iam:DeleteRole",
        "iam:AttachRolePolicy",
        "iam:DetachRolePolicy",
        "iam:GetRole",
        "iam:GetRolePolicy",
        "iam:PutRolePolicy",
        "iam:DeleteRolePolicy",
        "iam:ListRolePolicies",
        "iam:ListAttachedRolePolicies",
        "iam:CreateOpenIDConnectProvider",
        "iam:DeleteOpenIDConnectProvider",
        "iam:GetOpenIDConnectProvider",
        "logs:*",
        "cloudwatch:*",
        "s3:*",
        "dynamodb:*",
        "kms:*"
      ],
      "Resource": "*"
    }
  ]
}
```
</details>

#### 4. Service Quotas Check

Verify AWS service limits for your region:

```bash
# Check EKS quota
aws service-quotas get-service-quota \
  --service-code eks \
  --quota-code L-1194D53C \
  --region us-east-1

# Check VPC quota
aws service-quotas get-service-quota \
  --service-code vpc \
  --quota-code L-F678F1CE \
  --region us-east-1
```

Required quotas:
- **EKS Clusters**: 5 per region
- **VPCs**: 5 per region
- **Elastic IPs**: 5 per region
- **NAT Gateways**: 5 per AZ
- **RDS Instances**: 40 per region

---

## 🚀 Quick Start

Get up and running in 15 minutes with the development environment.

### Step 1: Clone and Navigate

```bash
cd terraform/aws
```

### Step 2: Initialize Terraform

```bash
# Initialize Terraform (downloads providers and modules)
terraform init

# Expected output:
# Initializing the backend...
# Initializing modules...
# Initializing provider plugins...
# Terraform has been successfully initialized!
```

### Step 3: Create Environment Variables File

```bash
# Create dev.tfvars file
cat > dev.tfvars <<EOF
aws_region   = "us-east-1"
environment  = "dev"
project_name = "stockxpress"
owner        = "DevOps Team"
cost_center  = "Engineering"

# Use smaller instances for dev
rds_instance_class         = "db.t3.small"
documentdb_instance_class  = "db.t3.medium"
redis_node_type            = "cache.t3.small"
kafka_broker_instance_type = "kafka.t3.small"

eks_node_groups = {
  general = {
    desired_size   = 2
    min_size       = 1
    max_size       = 4
    instance_types = ["t3.medium"]
    capacity_type  = "ON_DEMAND"
    disk_size      = 30
  }
}
EOF
```

### Step 4: Review Infrastructure Plan

```bash
# Generate execution plan
terraform plan -var-file=dev.tfvars -out=tfplan

# Review the plan carefully
# Expected resources: ~80-100 resources to be created
```

### Step 5: Deploy Infrastructure

```bash
# Apply the configuration
terraform apply tfplan

# This will take approximately 15-20 minutes
# Coffee break recommended ☕
```

### Step 6: Verify Deployment

```bash
# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name stockxpress-dev-eks

# Verify cluster access
kubectl get nodes

# Expected output:
# NAME                         STATUS   ROLES    AGE   VERSION
# ip-10-0-11-xxx.ec2.internal  Ready    <none>   5m    v1.28.x
# ip-10-0-12-xxx.ec2.internal  Ready    <none>   5m    v1.28.x
```

### Step 7: View Outputs

```bash
# Display all outputs
terraform output

# Get specific output (e.g., ECR URLs)
terraform output ecr_repository_urls

# Get kubectl configuration command
terraform output -raw configure_kubectl
```

---

## 🗄️ State Management Setup

### Why Remote State?

Remote state is **critical** for:
- ✅ Team collaboration
- ✅ State locking (prevents concurrent modifications)
- ✅ Encryption at rest
- ✅ Version history
- ✅ Disaster recovery

### One-Time Backend Setup

#### Step 1: Create S3 Bucket and DynamoDB Table

```bash
#!/bin/bash
# File: setup-backend.sh

REGION="us-east-1"
PROJECT="stockxpress"
BUCKET_NAME="${PROJECT}-terraform-state-${RANDOM}"
DYNAMODB_TABLE="${PROJECT}-terraform-locks"

# Create S3 bucket for state
echo "Creating S3 bucket: $BUCKET_NAME"
aws s3api create-bucket \
  --bucket $BUCKET_NAME \
  --region $REGION

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket $BUCKET_NAME \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket $BUCKET_NAME \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      },
      "BucketKeyEnabled": true
    }]
  }'

# Block public access
aws s3api put-public-access-block \
  --bucket $BUCKET_NAME \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# Enable lifecycle policy (optional - keeps only last 30 versions)
aws s3api put-bucket-lifecycle-configuration \
  --bucket $BUCKET_NAME \
  --lifecycle-configuration '{
    "Rules": [{
      "Id": "DeleteOldVersions",
      "Status": "Enabled",
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 30
      }
    }]
  }'

# Create DynamoDB table for state locking
echo "Creating DynamoDB table: $DYNAMODB_TABLE"
aws dynamodb create-table \
  --table-name $DYNAMODB_TABLE \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --region $REGION

# Wait for table to be active
aws dynamodb wait table-exists --table-name $DYNAMODB_TABLE --region $REGION

echo ""
echo "Backend setup complete!"
echo "Bucket: $BUCKET_NAME"
echo "Table: $DYNAMODB_TABLE"
echo ""
echo "Update main.tf backend configuration with these values."
```

#### Step 2: Make Script Executable and Run

```bash
chmod +x setup-backend.sh
./setup-backend.sh
```

#### Step 3: Update Backend Configuration in main.tf

Uncomment and update the backend block in `main.tf`:

```hcl
terraform {
  required_version = ">= 1.5.0"
  
  # Remote state storage
  backend "s3" {
    bucket         = "stockxpress-terraform-state-12345"  # Use your bucket name
    key            = "eks/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "stockxpress-terraform-locks"
    
    # Optional: Use KMS for additional encryption
    # kms_key_id     = "arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012"
  }
}
```

#### Step 4: Migrate Existing State (if applicable)

```bash
# Re-initialize with backend configuration
terraform init -migrate-state

# Terraform will ask: "Do you want to copy existing state to the new backend?"
# Answer: yes

# Verify state is in S3
aws s3 ls s3://stockxpress-terraform-state-12345/eks/
```

### Multi-Environment State Structure

For production deployments, use separate state files per environment:

```hcl
# Production
backend "s3" {
  bucket         = "stockxpress-terraform-state"
  key            = "production/eks/terraform.tfstate"
  region         = "us-east-1"
  encrypt        = true
  dynamodb_table = "stockxpress-terraform-locks"
}

# Staging
backend "s3" {
  bucket         = "stockxpress-terraform-state"
  key            = "staging/eks/terraform.tfstate"
  region         = "us-east-1"
  encrypt        = true
  dynamodb_table = "stockxpress-terraform-locks"
}

# Dev
backend "s3" {
  bucket         = "stockxpress-terraform-state"
  key            = "dev/eks/terraform.tfstate"
  region         = "us-east-1"
  encrypt        = true
  dynamodb_table = "stockxpress-terraform-locks"
}
```

### State Management Best Practices

1. **Never commit state files to Git**
   ```bash
   # .gitignore
   *.tfstate
   *.tfstate.backup
   .terraform/
   ```

2. **Enable state locking** - Always use DynamoDB for locking
3. **Use workspaces or separate state files** for different environments
4. **Regular backups** - S3 versioning provides automatic backups
5. **Restrict access** - Use IAM policies to control who can modify state

---

## 🌍 Environment-Specific Deployment

### Environment Strategy

We recommend a three-tier approach:

| Environment | Purpose | Characteristics |
|-------------|---------|----------------|
| **Development** | Feature development, testing | Smaller instances, single AZ, cost-optimized |
| **Staging** | Pre-production validation | Production-like, multi-AZ |
| **Production** | Live workloads | High availability, multi-AZ, auto-scaling |

### Development Environment

**File: `dev.tfvars`**

```hcl
# dev.tfvars - Cost-optimized development environment

aws_region   = "us-east-1"
environment  = "dev"
project_name = "stockxpress"
owner        = "DevOps Team"
cost_center  = "Engineering"

# Network Configuration
vpc_cidr             = "10.0.0.0/16"
availability_zones   = ["us-east-1a", "us-east-1b"]
public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs = ["10.0.11.0/24", "10.0.12.0/24"]
database_subnet_cidrs = ["10.0.21.0/24", "10.0.22.0/24"]

# EKS Configuration - Smaller nodes
eks_cluster_version = "1.28"
eks_node_groups = {
  general = {
    desired_size   = 2
    min_size       = 1
    max_size       = 4
    instance_types = ["t3.medium"]
    capacity_type  = "ON_DEMAND"
    disk_size      = 30
  }
  spot = {
    desired_size   = 1
    min_size       = 0
    max_size       = 3
    instance_types = ["t3.medium", "t3a.medium"]
    capacity_type  = "SPOT"
    disk_size      = 30
  }
}

# RDS Configuration - Smaller, single AZ
rds_instance_class    = "db.t3.small"
rds_allocated_storage = 20
mysql_engine_version  = "8.0"

# DocumentDB Configuration
documentdb_instance_class = "db.t3.medium"

# ElastiCache Configuration - Single node
redis_node_type      = "cache.t3.small"
redis_engine_version = "7.0"

# MSK Configuration - Smaller brokers
kafka_version              = "3.5.1"
kafka_broker_instance_type = "kafka.t3.small"
```

**Deploy Development:**

```bash
terraform init
terraform workspace new dev  # If using workspaces
terraform plan -var-file=dev.tfvars
terraform apply -var-file=dev.tfvars
```

### Staging Environment

**File: `staging.tfvars`**

```hcl
# staging.tfvars - Production-like environment for testing

aws_region   = "us-east-1"
environment  = "staging"
project_name = "stockxpress"
owner        = "DevOps Team"
cost_center  = "Engineering"

# Network Configuration - 3 AZs
vpc_cidr             = "10.1.0.0/16"
availability_zones   = ["us-east-1a", "us-east-1b", "us-east-1c"]
public_subnet_cidrs  = ["10.1.1.0/24", "10.1.2.0/24", "10.1.3.0/24"]
private_subnet_cidrs = ["10.1.11.0/24", "10.1.12.0/24", "10.1.13.0/24"]
database_subnet_cidrs = ["10.1.21.0/24", "10.1.22.0/24", "10.1.23.0/24"]

# EKS Configuration - Medium nodes
eks_cluster_version = "1.28"
eks_node_groups = {
  general = {
    desired_size   = 3
    min_size       = 2
    max_size       = 6
    instance_types = ["t3.large"]
    capacity_type  = "ON_DEMAND"
    disk_size      = 40
  }
  spot = {
    desired_size   = 2
    min_size       = 0
    max_size       = 5
    instance_types = ["t3.large", "t3a.large"]
    capacity_type  = "SPOT"
    disk_size      = 40
  }
}

# RDS Configuration - Medium, Multi-AZ
rds_instance_class    = "db.t3.medium"
rds_allocated_storage = 50
mysql_engine_version  = "8.0"

# DocumentDB Configuration - Multi-AZ
documentdb_instance_class = "db.r6g.large"

# ElastiCache Configuration - Cluster mode
redis_node_type      = "cache.t3.medium"
redis_engine_version = "7.0"

# MSK Configuration
kafka_version              = "3.5.1"
kafka_broker_instance_type = "kafka.m5.large"
```

**Deploy Staging:**

```bash
terraform workspace new staging  # If using workspaces
terraform plan -var-file=staging.tfvars
terraform apply -var-file=staging.tfvars
```

### Production Environment

**File: `production.tfvars`**

```hcl
# production.tfvars - High-availability production environment

aws_region   = "us-east-1"
environment  = "production"
project_name = "stockxpress"
owner        = "Platform Team"
cost_center  = "Production"

# Network Configuration - 3 AZs for HA
vpc_cidr             = "10.2.0.0/16"
availability_zones   = ["us-east-1a", "us-east-1b", "us-east-1c"]
public_subnet_cidrs  = ["10.2.1.0/24", "10.2.2.0/24", "10.2.3.0/24"]
private_subnet_cidrs = ["10.2.11.0/24", "10.2.12.0/24", "10.2.13.0/24"]
database_subnet_cidrs = ["10.2.21.0/24", "10.2.22.0/24", "10.2.23.0/24"]

# EKS Configuration - Production-grade nodes
eks_cluster_version = "1.28"
eks_node_groups = {
  general = {
    desired_size   = 4
    min_size       = 3
    max_size       = 10
    instance_types = ["m5.xlarge"]
    capacity_type  = "ON_DEMAND"
    disk_size      = 100
  }
  spot = {
    desired_size   = 3
    min_size       = 1
    max_size       = 8
    instance_types = ["m5.xlarge", "m5a.xlarge", "m5n.xlarge"]
    capacity_type  = "SPOT"
    disk_size      = 100
  }
}

# RDS Configuration - Production, Multi-AZ with automated backups
rds_instance_class    = "db.r6g.xlarge"
rds_allocated_storage = 100
mysql_engine_version  = "8.0"

# DocumentDB Configuration - Multi-AZ cluster
documentdb_instance_class = "db.r6g.xlarge"

# ElastiCache Configuration - Redis cluster with replicas
redis_node_type      = "cache.r6g.large"
redis_engine_version = "7.0"

# MSK Configuration - Production Kafka cluster
kafka_version              = "3.5.1"
kafka_broker_instance_type = "kafka.m5.xlarge"
```

**Deploy Production:**

```bash
# Use separate directory or workspace
terraform workspace new production
terraform plan -var-file=production.tfvars

# Require manual approval
terraform apply -var-file=production.tfvars

# Or with additional safeguards
terraform plan -var-file=production.tfvars -out=prod.tfplan
# Review plan thoroughly, get peer review
terraform apply prod.tfplan
```

### Workspace Management

```bash
# List workspaces
terraform workspace list

# Create new workspace
terraform workspace new <environment>

# Switch workspace
terraform workspace select <environment>

# Show current workspace
terraform workspace show

# Delete workspace (must be empty)
terraform workspace delete <environment>
```

---

## 📚 Module Documentation

The infrastructure is organized into reusable modules:

### Module: VPC

**Location:** `./modules/vpc`

**Purpose:** Creates isolated network infrastructure with public, private, and database subnets.

**Resources Created:**
- VPC with DNS support
- Internet Gateway
- NAT Gateways (one per AZ)
- Public subnets (for ALB, NAT)
- Private subnets (for EKS nodes, MSK)
- Database subnets (for RDS, DocumentDB)
- Route tables and associations
- VPC endpoints (optional)

**Inputs:**
```hcl
project_name         = "stockxpress"
environment          = "dev"
vpc_cidr             = "10.0.0.0/16"
availability_zones   = ["us-east-1a", "us-east-1b", "us-east-1c"]
public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
private_subnet_cidrs = ["10.0.11.0/24", "10.0.12.0/24", "10.0.13.0/24"]
```

**Outputs:**
- `vpc_id` - VPC identifier
- `private_subnet_ids` - List of private subnet IDs
- `public_subnet_ids` - List of public subnet IDs
- `database_subnet_ids` - List of database subnet IDs

### Module: EKS

**Location:** `./modules/eks`

**Purpose:** Provisions managed Kubernetes cluster with node groups.

**Resources Created:**
- EKS cluster
- IAM roles for cluster and nodes
- Security groups
- OIDC provider for IRSA
- Managed node groups (on-demand and spot)
- Launch templates

**Inputs:**
```hcl
cluster_name       = "stockxpress-dev-eks"
cluster_version    = "1.28"
vpc_id             = module.vpc.vpc_id
private_subnet_ids = module.vpc.private_subnet_ids
node_groups        = {...}
```

**Outputs:**
- `cluster_name` - EKS cluster name
- `cluster_endpoint` - API server endpoint
- `cluster_certificate_authority_data` - CA cert for kubectl
- `cluster_oidc_issuer_url` - OIDC provider URL

**Features:**
- Control plane logging enabled
- Private endpoint access
- Public endpoint access (configurable)
- IRSA (IAM Roles for Service Accounts)
- Managed node groups with auto-scaling

### Module: RDS

**Location:** `./modules/rds`

**Purpose:** Creates MySQL databases for Order and Inventory services.

**Resources Created:**
- RDS MySQL instances (x2)
- DB subnet groups
- Security groups
- Parameter groups
- Random passwords (stored in outputs)
- Automated backups
- Multi-AZ (production)

**Inputs:**
```hcl
order_db_config = {
  instance_class    = "db.t3.medium"
  allocated_storage = 50
  engine_version    = "8.0"
  database_name     = "order_service"
  username          = "orderadmin"
  multi_az          = true
}
```

**Outputs:**
- `order_db_endpoint` - Connection endpoint
- `order_db_password` - Generated password (sensitive)
- `inventory_db_endpoint` - Connection endpoint
- `inventory_db_password` - Generated password (sensitive)

**Features:**
- Automated backups (7-day retention)
- Encrypted at rest
- Performance Insights enabled
- Enhanced monitoring

### Module: DocumentDB

**Location:** `./modules/documentdb`

**Purpose:** MongoDB-compatible database for Product service.

**Resources Created:**
- DocumentDB cluster
- Cluster instances
- Subnet group
- Security groups
- Parameter groups

**Features:**
- MongoDB 5.0 compatible
- Automated backups
- Encryption at rest
- Multi-AZ for production

### Module: ElastiCache

**Location:** `./modules/elasticache`

**Purpose:** Redis cluster for Inventory service caching.

**Resources Created:**
- ElastiCache Redis cluster
- Subnet group
- Security groups
- Parameter groups

**Features:**
- Redis 7.0
- Automatic failover (cluster mode)
- Encryption in-transit
- Encryption at rest

### Module: MSK

**Location:** `./modules/msk`

**Purpose:** Managed Kafka cluster for event-driven architecture.

**Resources Created:**
- MSK cluster
- Configuration
- Security groups
- CloudWatch logging

**Features:**
- Apache Kafka 3.5.1
- TLS encryption
- IAM authentication
- CloudWatch monitoring
- Multi-AZ deployment

### Module: ECR

**Location:** `./modules/ecr`

**Purpose:** Docker container registries for microservices.

**Resources Created:**
- ECR repositories for each service:
  - discovery-server
  - api-gateway
  - product-service
  - inventory-service
  - order-service
  - notification-service

**Features:**
- Scan on push
- Encryption at rest
- Image tag immutability (optional)
- Lifecycle policies

### Module: Secrets Manager

**Location:** `./modules/secrets`

**Purpose:** Secure storage for database passwords and credentials.

**Resources Created:**
- Secrets for all database passwords
- Secrets for ElastiCache auth token
- IAM policies for secret access

**Features:**
- Automatic rotation (configurable)
- Encryption with KMS
- Version management

### Module: K8s Addons

**Location:** `./modules/k8s-addons`

**Purpose:** Essential Kubernetes operational tools.

**Resources Created:**
- Metrics Server (resource metrics)
- Cluster Autoscaler (node auto-scaling)
- AWS Load Balancer Controller (ALB/NLB)
- External Secrets Operator (sync from Secrets Manager)
- Prometheus (metrics collection)
- Grafana (visualization)

---

## 🔄 Complete Deployment Workflow

Comprehensive step-by-step deployment guide from infrastructure provisioning to application deployment.

### Phase 1: Infrastructure Provisioning

#### Step 1: Repository Setup

```bash
# Clone repository
git clone <repository-url>
cd stockxpress/terraform/aws

# Verify Terraform files
ls -la
# Expected: main.tf, variables.tf, outputs.tf
```

#### Step 2: Backend Configuration

```bash
# Run backend setup script (one-time)
./setup-backend.sh

# Update main.tf with backend details
# Uncomment and configure the backend block
```

#### Step 3: Initialize Terraform

```bash
# Download providers and initialize
terraform init

# Validate configuration
terraform validate

# Format code (optional)
terraform fmt -recursive
```

#### Step 4: Plan Infrastructure

```bash
# Create execution plan for dev environment
terraform plan -var-file=dev.tfvars -out=tfplan

# Review plan output carefully:
# - Number of resources to be created
# - Resource types
# - Configuration values
# - Estimated costs (if using Infracost)
```

#### Step 5: Apply Infrastructure

```bash
# Apply the plan
terraform apply tfplan

# ⏱️ Expected duration: 15-20 minutes
# The longest operations:
# - NAT Gateways: ~3 minutes
# - EKS Cluster: ~10 minutes
# - RDS Instances: ~5 minutes
# - DocumentDB: ~10 minutes
# - MSK Cluster: ~15 minutes
```

#### Step 6: Capture Outputs

```bash
# Save all outputs to file
terraform output -json > infrastructure-outputs.json

# Extract specific values
export CLUSTER_NAME=$(terraform output -raw eks_cluster_name)
export AWS_REGION=$(terraform output -raw aws_region || echo "us-east-1")

echo "Cluster: $CLUSTER_NAME"
echo "Region: $AWS_REGION"
```

### Phase 2: Kubernetes Configuration

#### Step 7: Configure kubectl

```bash
# Update kubeconfig
aws eks update-kubeconfig \
  --region $AWS_REGION \
  --name $CLUSTER_NAME

# Verify cluster access
kubectl cluster-info
kubectl get nodes

# Expected output: 3-6 nodes in Ready state
```

#### Step 8: Verify Add-ons

```bash
# Check metrics server
kubectl get deployment metrics-server -n kube-system

# Check cluster autoscaler
kubectl get deployment cluster-autoscaler -n kube-system

# Check AWS Load Balancer Controller
kubectl get deployment aws-load-balancer-controller -n kube-system

# Check External Secrets Operator
kubectl get deployment external-secrets -n external-secrets-system
```

#### Step 9: Create Application Namespace

```bash
# Create namespace
kubectl create namespace stockxpress

# Set as default (optional)
kubectl config set-context --current --namespace=stockxpress

# Verify
kubectl get namespace stockxpress
```

#### Step 10: Configure External Secrets

```bash
# Create SecretStore for AWS Secrets Manager
cat <<EOF | kubectl apply -f -
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: stockxpress
spec:
  provider:
    aws:
      service: SecretsManager
      region: $AWS_REGION
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa
EOF

# Verify SecretStore
kubectl get secretstore -n stockxpress
```

#### Step 11: Create External Secrets

```bash
# Create ExternalSecret for RDS Order DB
cat <<EOF | kubectl apply -f -
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: order-db-credentials
  namespace: stockxpress
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: order-db-secret
    creationPolicy: Owner
  data:
    - secretKey: password
      remoteRef:
        key: stockxpress-dev-rds-order-password
EOF

# Repeat for other secrets (inventory, documentdb, redis)

# Verify secrets are synced
kubectl get externalsecrets -n stockxpress
kubectl get secrets -n stockxpress
```

### Phase 3: Container Image Preparation

#### Step 12: Authenticate to ECR

```bash
# Get ECR login command
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  $(aws sts get-caller-identity --query Account --output text).dkr.ecr.$AWS_REGION.amazonaws.com

# Verify authentication
# Expected: "Login Succeeded"
```

#### Step 13: Build and Push Images

```bash
# Get ECR repository URLs
terraform output -json ecr_repository_urls | jq -r '.'

# Set variables
ECR_REGISTRY=$(aws sts get-caller-identity --query Account --output text).dkr.ecr.$AWS_REGION.amazonaws.com
IMAGE_TAG="v1.0.0"

# Build and push each service
cd ../../  # Back to project root

# Discovery Server
docker build -t $ECR_REGISTRY/stockxpress-discovery-server:$IMAGE_TAG \
  -f discovery-server/Dockerfile .
docker push $ECR_REGISTRY/stockxpress-discovery-server:$IMAGE_TAG

# API Gateway
docker build -t $ECR_REGISTRY/stockxpress-api-gateway:$IMAGE_TAG \
  -f api-gateway/Dockerfile .
docker push $ECR_REGISTRY/stockxpress-api-gateway:$IMAGE_TAG

# Product Service
docker build -t $ECR_REGISTRY/stockxpress-product-service:$IMAGE_TAG \
  -f product-service/Dockerfile .
docker push $ECR_REGISTRY/stockxpress-product-service:$IMAGE_TAG

# Inventory Service
docker build -t $ECR_REGISTRY/stockxpress-inventory-service:$IMAGE_TAG \
  -f inventory-service/Dockerfile .
docker push $ECR_REGISTRY/stockxpress-inventory-service:$IMAGE_TAG

# Order Service
docker build -t $ECR_REGISTRY/stockxpress-order-service:$IMAGE_TAG \
  -f order-service/Dockerfile .
docker push $ECR_REGISTRY/stockxpress-order-service:$IMAGE_TAG

# Notification Service
docker build -t $ECR_REGISTRY/stockxpress-notification-service:$IMAGE_TAG \
  -f notification-service/Dockerfile .
docker push $ECR_REGISTRY/stockxpress-notification-service:$IMAGE_TAG
```

#### Step 14: Verify Images in ECR

```bash
# List images in each repository
for repo in discovery-server api-gateway product-service inventory-service order-service notification-service; do
  echo "Repository: stockxpress-$repo"
  aws ecr list-images --repository-name "stockxpress-$repo" --region $AWS_REGION
  echo ""
done
```

### Phase 4: Infrastructure Deployment

#### Step 15: Deploy Infrastructure Components

```bash
cd k8s/base/infrastructure

# Deploy MySQL for Order Service
kubectl apply -f mysql-order-statefulset.yml

# Deploy MySQL for Inventory Service
kubectl apply -f mysql-inventory-statefulset.yml

# Deploy MongoDB for Product Service
kubectl apply -f mongodb-statefulset.yml

# Deploy Redis
kubectl apply -f redis-deployment.yml

# Deploy Kafka (using MSK endpoints from Terraform)
kubectl apply -f kafka-statefulset.yml

# Deploy Zipkin for distributed tracing
kubectl apply -f zipkin-deployment.yml

# Wait for all pods to be ready
kubectl wait --for=condition=ready pod -l app=mysql-order --timeout=300s
kubectl wait --for=condition=ready pod -l app=mysql-inventory --timeout=300s
kubectl wait --for=condition=ready pod -l app=mongodb --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis --timeout=300s
```

#### Step 16: Update ConfigMaps with Terraform Outputs

```bash
# Extract endpoints from Terraform
export RDS_ORDER_ENDPOINT=$(cd ../../../terraform/aws && terraform output -raw rds_order_endpoint)
export RDS_INVENTORY_ENDPOINT=$(cd ../../../terraform/aws && terraform output -raw rds_inventory_endpoint)
export DOCUMENTDB_ENDPOINT=$(cd ../../../terraform/aws && terraform output -raw documentdb_endpoint)
export REDIS_ENDPOINT=$(cd ../../../terraform/aws && terraform output -raw redis_endpoint)
export MSK_BROKERS=$(cd ../../../terraform/aws && terraform output -raw msk_bootstrap_brokers)

# Update service ConfigMap
cd ../services
envsubst < service-configmaps.yml | kubectl apply -f -
```

### Phase 5: Application Deployment

#### Step 17: Deploy Microservices

```bash
# Deploy in dependency order

# 1. Discovery Server (Eureka)
kubectl apply -f discovery-server-deployment.yml
kubectl apply -f discovery-server-service.yml
kubectl wait --for=condition=ready pod -l app=discovery-server --timeout=300s

# 2. API Gateway
kubectl apply -f api-gateway-deployment.yml
kubectl apply -f api-gateway-service.yml
kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=300s

# 3. Product Service
kubectl apply -f product-service-deployment.yml
kubectl apply -f product-service-service.yml
kubectl wait --for=condition=ready pod -l app=product-service --timeout=300s

# 4. Inventory Service
kubectl apply -f inventory-service-deployment.yml
kubectl apply -f inventory-service-service.yml
kubectl wait --for=condition=ready pod -l app=inventory-service --timeout=300s

# 5. Order Service
kubectl apply -f order-service-deployment.yml
kubectl apply -f order-service-service.yml
kubectl wait --for=condition=ready pod -l app=order-service --timeout=300s

# 6. Notification Service
kubectl apply -f notification-service-deployment.yml
kubectl apply -f notification-service-service.yml
kubectl wait --for=condition=ready pod -l app=notification-service --timeout=300s
```

#### Step 18: Deploy Ingress

```bash
# Apply Ingress configuration (creates ALB)
kubectl apply -f ingress.yml

# Wait for ALB to be provisioned (can take 3-5 minutes)
kubectl get ingress stockxpress-ingress -w

# Get ALB DNS name
export ALB_DNS=$(kubectl get ingress stockxpress-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "Application URL: http://$ALB_DNS"
```

#### Step 19: Verify Deployment

```bash
# Check all pods
kubectl get pods -n stockxpress

# Check all services
kubectl get svc -n stockxpress

# Check ingress
kubectl get ingress -n stockxpress

# View logs for specific service
kubectl logs -l app=api-gateway --tail=100

# Check service endpoints
kubectl get endpoints -n stockxpress
```

#### Step 20: Health Checks

```bash
# Test Eureka Dashboard
curl http://$ALB_DNS/eureka/web

# Test API Gateway health
curl http://$ALB_DNS/actuator/health

# Test Product Service
curl http://$ALB_DNS/api/products

# Test Inventory Service
curl http://$ALB_DNS/api/inventory

# Test Order Service
curl http://$ALB_DNS/api/orders
```

### Phase 6: Monitoring Setup

#### Step 21: Access Grafana

```bash
# Port-forward to Grafana
kubectl port-forward -n kube-system svc/grafana 3000:80 &

# Access Grafana at http://localhost:3000
# Default credentials: admin/admin

# Import dashboards from grafana/dashboards/
```

#### Step 22: Access Prometheus

```bash
# Port-forward to Prometheus
kubectl port-forward -n kube-system svc/prometheus-server 9090:80 &

# Access Prometheus at http://localhost:9090
```

### Post-Deployment Checklist

- [ ] All pods are in Running state
- [ ] All services have endpoints
- [ ] Ingress has external IP/DNS
- [ ] Health checks pass for all services
- [ ] Eureka shows all registered services
- [ ] Grafana dashboards are accessible
- [ ] Prometheus is collecting metrics
- [ ] CloudWatch logs are being received
- [ ] Database connections are working
- [ ] Kafka topics are created
- [ ] Redis cache is accessible

---

## 💰 Cost Estimation

### Monthly Cost Breakdown (US East 1)

#### Development Environment

| Service | Resource | Quantity | Unit Cost | Monthly Cost |
|---------|----------|----------|-----------|-------------|
| **EKS** | Control Plane | 1 | $73/month | $73 |
| **EKS** | t3.medium nodes | 2 | $30/month | $60 |
| **EC2** | NAT Gateway | 2 | $32/month | $64 |
| **EC2** | NAT Data Transfer | 100GB | $0.045/GB | $5 |
| **RDS** | db.t3.small MySQL | 2 | $25/month | $50 |
| **DocumentDB** | db.t3.medium | 1 | $70/month | $70 |
| **ElastiCache** | cache.t3.small | 1 | $24/month | $24 |
| **MSK** | kafka.t3.small | 2 | $37/month | $74 |
| **ECR** | Storage | 50GB | $0.10/GB | $5 |
| **Secrets Manager** | Secrets | 6 | $0.40/month | $2.40 |
| **CloudWatch** | Logs | 10GB | $0.50/GB | $5 |
| **Data Transfer** | Outbound | 50GB | $0.09/GB | $5 |
| **Total** | | | | **~$437/month** |

#### Staging Environment

| Service | Resource | Quantity | Unit Cost | Monthly Cost |
|---------|----------|----------|-----------|-------------|
| **EKS** | Control Plane | 1 | $73/month | $73 |
| **EKS** | t3.large nodes | 3 | $60/month | $180 |
| **EC2** | NAT Gateway | 3 | $32/month | $96 |
| **RDS** | db.t3.medium MySQL (Multi-AZ) | 2 | $100/month | $200 |
| **DocumentDB** | db.r6g.large (Multi-AZ) | 2 | $190/month | $380 |
| **ElastiCache** | cache.t3.medium (Cluster) | 2 | $49/month | $98 |
| **MSK** | kafka.m5.large | 3 | $120/month | $360 |
| **Other** | ECR, Secrets, Logs, Transfer | - | - | $25 |
| **Total** | | | | **~$1,412/month** |

#### Production Environment

| Service | Resource | Quantity | Unit Cost | Monthly Cost |
|---------|----------|----------|-----------|-------------|
| **EKS** | Control Plane | 1 | $73/month | $73 |
| **EKS** | m5.xlarge nodes | 4 | $140/month | $560 |
| **EKS** | m5.xlarge spot nodes | 3 | $50/month | $150 |
| **EC2** | NAT Gateway | 3 | $32/month | $96 |
| **RDS** | db.r6g.xlarge MySQL (Multi-AZ) | 2 | $400/month | $800 |
| **DocumentDB** | db.r6g.xlarge (3-node cluster) | 3 | $380/month | $1,140 |
| **ElastiCache** | cache.r6g.large (Cluster) | 3 | $158/month | $474 |
| **MSK** | kafka.m5.xlarge (Multi-AZ) | 3 | $240/month | $720 |
| **S3** | Terraform state, backups | - | - | $10 |
| **DynamoDB** | State locking | - | - | $1 |
| **CloudWatch** | Logs, metrics | - | - | $50 |
| **Data Transfer** | Outbound | 500GB | $0.09/GB | $45 |
| **Total** | | | | **~$4,119/month** |

### Cost Optimization Strategies

#### 1. Use Spot Instances
```hcl
# Save 60-70% on compute
eks_node_groups = {
  spot = {
    capacity_type  = "SPOT"
    instance_types = ["m5.xlarge", "m5a.xlarge", "m5n.xlarge"]
  }
}
```
**Savings:** ~$90/month per node

#### 2. Reserved Instances for Databases
```bash
# 1-year reservation saves 35%
# 3-year reservation saves 60%
```
**Savings:** $200-400/month on production RDS

#### 3. Auto-Scaling
```hcl
# Scale down during off-hours
min_size = 1  # Night/weekend
max_size = 10 # Peak hours
```
**Savings:** ~$120/month

#### 4. NAT Gateway Optimization
```bash
# Use single NAT Gateway for dev/staging
# Use VPC endpoints to avoid NAT data charges
```
**Savings:** ~$64/month per environment

#### 5. Storage Optimization
```bash
# Enable ECR lifecycle policies
# Delete old RDS snapshots
# Use S3 Intelligent-Tiering
```
**Savings:** ~$20-50/month

#### 6. Right-Sizing
```bash
# Monitor resource utilization
# Use AWS Compute Optimizer recommendations
# Downsize overprovisioned resources
```
**Savings:** 15-30% overall

### Cost Monitoring

```bash
# Set up AWS Budgets
aws budgets create-budget \
  --account-id $(aws sts get-caller-identity --query Account --output text) \
  --budget file://budget.json \
  --notifications-with-subscribers file://notifications.json

# Enable Cost Anomaly Detection
aws ce create-anomaly-monitor \
  --anomaly-monitor file://anomaly-monitor.json
```

### Using Infracost

```bash
# Install Infracost
brew install infracost

# Register (free)
infracost auth login

# Generate cost estimate
infracost breakdown --path . --var-file=production.tfvars

# Compare costs between environments
infracost diff --path . --var-file=dev.tfvars
```

---

## 🔒 Security Best Practices

### 1. Network Security

#### VPC Configuration
```hcl
# ✅ Use private subnets for all workloads
# ✅ Use NAT Gateways for outbound internet access
# ✅ Database subnets have no internet access
# ✅ Security groups follow least-privilege
```

#### Security Group Rules
```bash
# Example: RDS security group
# Only allow access from EKS nodes
resource "aws_security_group_rule" "rds_from_eks" {
  type                     = "ingress"
  from_port                = 3306
  to_port                  = 3306
  protocol                 = "tcp"
  security_group_id        = aws_security_group.rds.id
  source_security_group_id = aws_security_group.eks_nodes.id
}
```

#### Network Policies
```yaml
# Apply network policies in Kubernetes
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all-ingress
  namespace: stockxpress
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  # Then create specific allow rules
```

### 2. Encryption

#### At Rest
- ✅ RDS: Encrypted with KMS
- ✅ DocumentDB: Encrypted with KMS
- ✅ ElastiCache: Encrypted at rest
- ✅ EKS: Encrypted EBS volumes
- ✅ S3: Server-side encryption
- ✅ Secrets Manager: KMS encryption

#### In Transit
- ✅ ALB: HTTPS/TLS termination
- ✅ RDS: SSL/TLS connections
- ✅ DocumentDB: TLS enabled
- ✅ MSK: TLS encryption
- ✅ ElastiCache: Transit encryption

```bash
# Generate TLS certificate for ALB
aws acm request-certificate \
  --domain-name "*.stockxpress.example.com" \
  --validation-method DNS \
  --region us-east-1
```

### 3. Access Control

#### IAM Roles for Service Accounts (IRSA)
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: product-service-sa
  namespace: stockxpress
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT:role/ProductServiceRole
```

#### Principle of Least Privilege
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:ACCOUNT:secret:product-db-*"
      ]
    }
  ]
}
```

### 4. Secrets Management

#### Use AWS Secrets Manager
```bash
# Never hardcode secrets
# ❌ Bad
export DB_PASSWORD="hardcoded123"

# ✅ Good - Use Secrets Manager
aws secretsmanager get-secret-value \
  --secret-id stockxpress/dev/rds-password \
  --query SecretString --output text
```

#### External Secrets Operator
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: db-credentials
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
  data:
    - secretKey: password
      remoteRef:
        key: stockxpress/dev/rds-password
```

### 5. Audit & Compliance

#### Enable CloudTrail
```bash
aws cloudtrail create-trail \
  --name stockxpress-audit \
  --s3-bucket-name stockxpress-cloudtrail-logs

aws cloudtrail start-logging --name stockxpress-audit
```

#### Enable VPC Flow Logs
```hcl
resource "aws_flow_log" "vpc" {
  vpc_id          = module.vpc.vpc_id
  traffic_type    = "ALL"
  log_destination = aws_cloudwatch_log_group.vpc_flow_log.arn
  iam_role_arn    = aws_iam_role.vpc_flow_log.arn
}
```

#### Enable EKS Control Plane Logging
```hcl
cluster_enabled_log_types = [
  "api",
  "audit",
  "authenticator",
  "controllerManager",
  "scheduler"
]
```

### 6. Image Security

#### ECR Image Scanning
```hcl
resource "aws_ecr_repository" "service" {
  image_scanning_configuration {
    scan_on_push = true
  }
  
  image_tag_mutability = "IMMUTABLE"
}
```

#### Scan Images Locally
```bash
# Use Trivy for vulnerability scanning
trivy image stockxpress-api-gateway:latest

# Use Snyk
snyk container test stockxpress-api-gateway:latest
```

### 7. Pod Security

#### Pod Security Standards
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: stockxpress
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

#### Security Context
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  capabilities:
    drop:
      - ALL
  readOnlyRootFilesystem: true
```

### 8. Backup & Disaster Recovery

#### RDS Automated Backups
```hcl
backup_retention_period = 7  # days
backup_window          = "03:00-04:00"
maintenance_window     = "sun:04:00-sun:05:00"
```

#### Point-in-Time Recovery
```bash
# Restore RDS to specific timestamp
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier stockxpress-order-db \
  --target-db-instance-identifier stockxpress-order-db-restored \
  --restore-time 2024-01-15T10:30:00Z
```

### Security Checklist

- [ ] All databases are in private subnets
- [ ] Encryption at rest enabled for all data stores
- [ ] TLS/SSL enabled for all connections
- [ ] Security groups follow least-privilege
- [ ] IAM roles use least-privilege policies
- [ ] Secrets stored in Secrets Manager (not environment variables)
- [ ] CloudTrail enabled for audit logging
- [ ] VPC Flow Logs enabled
- [ ] EKS control plane logging enabled
- [ ] ECR image scanning enabled
- [ ] Pod Security Standards enforced
- [ ] Network policies applied
- [ ] Automated backups configured
- [ ] MFA enabled for AWS console access
- [ ] AWS Config rules enabled

---

## 🔧 Maintenance & Operations

### Regular Maintenance Tasks

#### Weekly
```bash
# Review CloudWatch alarms
aws cloudwatch describe-alarms --state-value ALARM

# Check for security vulnerabilities
aws ecr describe-image-scan-findings \
  --repository-name stockxpress-api-gateway \
  --image-id imageTag=latest

# Review cost reports
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-07 \
  --granularity DAILY \
  --metrics BlendedCost
```

#### Monthly
```bash
# Update EKS add-ons
kubectl apply -f k8s-addons/

# Review and rotate secrets
aws secretsmanager rotate-secret \
  --secret-id stockxpress/prod/rds-password

# Review IAM policies
aws iam generate-service-last-accessed-details

# Patch worker nodes (managed node groups do this automatically)
```

#### Quarterly
```bash
# Upgrade Kubernetes version
# 1. Upgrade control plane
aws eks update-cluster-version \
  --name stockxpress-prod-eks \
  --kubernetes-version 1.29

# 2. Upgrade node groups
aws eks update-nodegroup-version \
  --cluster-name stockxpress-prod-eks \
  --nodegroup-name general

# 3. Update kubectl and tools
brew upgrade kubectl helm terraform
```

### Scaling Operations

#### Manual Node Scaling
```bash
# Scale node group
aws eks update-nodegroup-config \
  --cluster-name stockxpress-prod-eks \
  --nodegroup-name general \
  --scaling-config minSize=5,maxSize=15,desiredSize=8
```

#### Application Auto-Scaling
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Backup & Restore

#### Manual RDS Snapshot
```bash
# Create snapshot
aws rds create-db-snapshot \
  --db-instance-identifier stockxpress-order-db \
  --db-snapshot-identifier order-db-snapshot-$(date +%Y%m%d)

# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier stockxpress-order-db-restored \
  --db-snapshot-identifier order-db-snapshot-20240115
```

#### Kubernetes Resource Backup
```bash
# Use Velero for K8s backups
helm install velero vmware-tanzu/velero \
  --namespace velero \
  --create-namespace \
  --set configuration.provider=aws \
  --set configuration.backupStorageLocation.bucket=stockxpress-velero-backups

# Create backup
velero backup create stockxpress-backup-$(date +%Y%m%d)

# Restore
velero restore create --from-backup stockxpress-backup-20240115
```

### Monitoring & Alerting

#### CloudWatch Alarms
```bash
# High CPU on EKS nodes
aws cloudwatch put-metric-alarm \
  --alarm-name eks-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold
```

#### Log Analysis
```bash
# Query CloudWatch Logs Insights
aws logs start-query \
  --log-group-name "/aws/eks/stockxpress-prod-eks/cluster" \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date +%s) \
  --query-string 'fields @timestamp, @message | filter @message like /ERROR/ | sort @timestamp desc'
```

---

## 🔍 Troubleshooting Guide

### Common Issues & Solutions

#### Issue 1: Terraform Apply Fails

**Symptom:**
```
Error: Error creating EKS Cluster: AccessDenied
```

**Solution:**
```bash
# Verify AWS credentials
aws sts get-caller-identity

# Check IAM permissions
aws iam simulate-principal-policy \
  --policy-source-arn $(aws sts get-caller-identity --query Arn --output text) \
  --action-names eks:CreateCluster

# Ensure you have the required permissions (see Prerequisites)
```

#### Issue 2: kubectl Cannot Connect

**Symptom:**
```
The connection to the server localhost:8080 was refused
```

**Solution:**
```bash
# Update kubeconfig
aws eks update-kubeconfig \
  --region us-east-1 \
  --name stockxpress-dev-eks

# Verify config
kubectl config current-context

# Test connection
kubectl cluster-info

# If still fails, check IAM permissions
aws eks describe-cluster --name stockxpress-dev-eks
```

#### Issue 3: Pods in Pending State

**Symptom:**
```bash
kubectl get pods
# NAME                    READY   STATUS    RESTARTS   AGE
# api-gateway-xxx         0/1     Pending   0          5m
```

**Diagnosis:**
```bash
# Check pod events
kubectl describe pod api-gateway-xxx

# Common causes:
# 1. Insufficient resources
kubectl top nodes

# 2. Node selector/affinity issues
kubectl get nodes --show-labels

# 3. PVC not bound
kubectl get pvc
```

**Solution:**
```bash
# Scale up node group
aws eks update-nodegroup-config \
  --cluster-name stockxpress-dev-eks \
  --nodegroup-name general \
  --scaling-config desiredSize=4

# Or adjust resource requests
kubectl edit deployment api-gateway
```

#### Issue 4: Database Connection Failed

**Symptom:**
```
Communications link failure: Connection refused
```

**Diagnosis:**
```bash
# Get RDS endpoint
terraform output rds_order_endpoint

# Test connection from pod
kubectl run -it --rm debug \
  --image=mysql:8.0 \
  --restart=Never \
  -- mysql -h <RDS_ENDPOINT> -u orderadmin -p

# Check security groups
aws ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=*rds*"
```

**Solution:**
```bash
# Verify security group allows traffic from EKS nodes
# Check that pods are using correct endpoint
kubectl get configmap service-config -o yaml

# Verify secret contains correct password
kubectl get secret order-db-secret -o yaml
```

#### Issue 5: Ingress/ALB Not Working

**Symptom:**
```
Ingress has no external IP/hostname
```

**Diagnosis:**
```bash
# Check ingress status
kubectl describe ingress stockxpress-ingress

# Check AWS Load Balancer Controller logs
kubectl logs -n kube-system \
  -l app.kubernetes.io/name=aws-load-balancer-controller

# Verify controller is running
kubectl get deployment -n kube-system aws-load-balancer-controller
```

**Solution:**
```bash
# Reinstall AWS Load Balancer Controller
helm upgrade aws-load-balancer-controller \
  eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=stockxpress-dev-eks

# Verify IAM role for service account
kubectl describe sa aws-load-balancer-controller -n kube-system
```

#### Issue 6: High Costs

**Symptom:**
AWS bill higher than expected

**Diagnosis:**
```bash
# Check cost by service
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --group-by Type=DIMENSION,Key=SERVICE

# Identify untagged resources
aws resourcegroupstaggingapi get-resources \
  --tag-filters Key=Environment,Values=dev
```

**Solution:**
```bash
# Review and right-size resources
# - Use AWS Compute Optimizer
# - Enable auto-scaling
# - Delete unused resources
# - Use Spot instances
# - Enable RDS reserved instances

# Find unused EBS volumes
aws ec2 describe-volumes \
  --filters Name=status,Values=available

# Delete unused snapshots
aws ec2 describe-snapshots --owner-ids self
```

#### Issue 7: MSK Connection Issues

**Symptom:**
```
Failed to connect to Kafka broker
```

**Diagnosis:**
```bash
# Get MSK bootstrap servers
terraform output msk_bootstrap_brokers

# Test from pod
kubectl run -it --rm kafka-test \
  --image=confluentinc/cp-kafka:latest \
  --restart=Never \
  -- kafka-broker-api-versions \
  --bootstrap-server <MSK_BROKERS>
```

**Solution:**
```bash
# Verify security group
# Verify pods can resolve MSK DNS
kubectl run -it --rm debug \
  --image=busybox \
  --restart=Never \
  -- nslookup <MSK_ENDPOINT>
```

### Debug Commands Cheat Sheet

```bash
# Kubernetes
kubectl get all -n stockxpress
kubectl describe pod <pod-name>
kubectl logs <pod-name> --previous
kubectl exec -it <pod-name> -- /bin/sh
kubectl top nodes
kubectl top pods

# AWS EKS
aws eks describe-cluster --name <cluster-name>
aws eks list-nodegroups --cluster-name <cluster-name>
aws eks describe-nodegroup --cluster-name <cluster> --nodegroup-name <name>

# AWS RDS
aws rds describe-db-instances
aws rds describe-db-clusters

# AWS Logs
aws logs tail /aws/eks/<cluster-name>/cluster --follow

# Terraform
terraform state list
terraform state show <resource>
terraform refresh
terraform plan -refresh-only
```

### Getting Help

1. **Check CloudWatch Logs**: All services log to CloudWatch
2. **Review EKS Control Plane Logs**: API server, audit, controller manager
3. **AWS Support**: File a support ticket for infrastructure issues
4. **Community**: Terraform AWS modules GitHub issues
5. **Documentation**:
   - [AWS EKS Best Practices](https://aws.github.io/aws-eks-best-practices/)
   - [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)

---

## ❓ FAQ

### General

**Q: How long does initial deployment take?**  
A: Approximately 20-25 minutes. EKS cluster creation takes ~10 minutes, MSK ~15 minutes, and RDS ~5 minutes.

**Q: Can I deploy in a different AWS region?**  
A: Yes, change the `aws_region` variable in your tfvars file.

**Q: Do I need a domain name?**  
A: Not required for initial deployment. ALB provides a DNS name. For production, use Route53 with a custom domain.

**Q: Can I use an existing VPC?**  
A: Yes, modify the VPC module to import existing VPC or remove it and pass VPC ID directly to other modules.

### Cost

**Q: What's the minimum cost to run this?**  
A: Development environment costs approximately $400-450/month. Consider using LocalStack or Minikube for local development.

**Q: How can I reduce costs?**  
A: Use Spot instances, single NAT Gateway, smaller RDS instances, auto-scaling, and tear down dev/staging overnight.

**Q: Does this qualify for AWS Free Tier?**  
A: Most services (EKS, RDS, DocumentDB) are not free tier eligible. You can use smaller instance types to minimize costs.

### Security

**Q: Are databases publicly accessible?**  
A: No, all databases are in private subnets with no internet access.

**Q: How are secrets managed?**  
A: Secrets are stored in AWS Secrets Manager and synced to Kubernetes using External Secrets Operator.

**Q: Is traffic encrypted?**  
A: Yes, encryption in transit (TLS) and at rest (KMS) is enabled for all services.

### Operations

**Q: How do I upgrade Kubernetes version?**  
A: Follow the quarterly maintenance guide above. Upgrade control plane first, then node groups.

**Q: Can I use kubectl from CI/CD?**  
A: Yes, use AWS IAM authenticator and ensure your CI/CD role has eks:DescribeCluster permission.

**Q: How do I access private databases?**  
A: Use kubectl port-forward or create a bastion host in public subnet.

### Disaster Recovery

**Q: What's the RTO/RPO?**  
A: With Multi-AZ RDS: RTO ~5 minutes, RPO ~5 minutes. With backups: RTO ~30 minutes, RPO ~1 hour.

**Q: How are backups managed?**  
A: RDS automated backups (7-day retention), manual snapshots, and Velero for Kubernetes resources.

**Q: Can I restore to a point in time?**  
A: Yes, RDS supports point-in-time recovery within the backup retention period.

---

## 📞 Support & Contributing

### Getting Support

- **Issues**: Open a GitHub issue
- **Questions**: Use GitHub Discussions
- **Security**: Email security@stockxpress.com

### Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

### Versioning

This project follows [Semantic Versioning](https://semver.org/).

---

## 📄 License

This project is licensed under the MIT License.

---

## 🙏 Acknowledgments

- AWS EKS team for excellent documentation
- Terraform community for AWS modules
- HashiCorp for Terraform

---

**Last Updated:** January 2024  
**Terraform Version:** 1.5+  
**AWS Provider Version:** 5.0+  
**EKS Version:** 1.28

---

## Quick Links

- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Project Architecture](../../ARCHITECTURE.md)
- [CI/CD Pipeline](../../CICD.md)
- [Deployment Guide](../../DEPLOYMENT_GUIDE.md)

---

**Happy Deploying! 🚀**
