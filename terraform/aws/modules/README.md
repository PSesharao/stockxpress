# AWS Terraform Modules

A collection of production-ready Terraform modules for deploying infrastructure on AWS.

## Available Modules

### [VPC Module](./vpc)

Creates a highly available VPC with public, private, and database subnets across multiple Availability Zones.

**Key Features:**
- Multi-AZ architecture (configurable 2-6 AZs)
- Public, private, and database subnet tiers
- NAT Gateways for private subnet internet access
- Proper EKS subnet tagging
- VPC Flow Logs (optional)
- DB subnet group for RDS

**Use Cases:**
- EKS cluster networking
- Multi-tier application architecture
- Microservices deployment

[View VPC Module Documentation →](./vpc/README.md)

### [EKS Module](./eks)

Deploys an Amazon EKS cluster with managed node groups, IRSA support, and essential addons.

**Key Features:**
- IRSA (IAM Roles for Service Accounts) support
- Managed node groups with auto-scaling
- KMS encryption for cluster secrets
- Security groups with least privilege
- Essential addons (VPC CNI, CoreDNS, kube-proxy, EBS CSI)
- Support for multiple node groups with taints and labels
- IMDSv2 enforcement

**Use Cases:**
- Container orchestration
- Microservices platform
- CI/CD workloads
- Spot instance optimization

[View EKS Module Documentation →](./eks/README.md)

### [RDS Module](./rds)

Provisions MySQL RDS instances with automated backups, encryption, and Multi-AZ support.

**Key Features:**
- Multi-AZ deployment for HA
- Automated backups with configurable retention
- KMS encryption at rest
- Enhanced monitoring and Performance Insights
- CloudWatch alarms
- Secrets Manager integration
- Custom parameter and option groups
- Storage autoscaling

**Use Cases:**
- Application databases
- Transactional workloads
- Multi-database architectures

[View RDS Module Documentation →](./rds/README.md)

## Module Structure

Each module follows a consistent structure:

```
module-name/
├── main.tf          # Main resource definitions
├── variables.tf     # Input variables
├── outputs.tf       # Output values
├── versions.tf      # Terraform and provider version constraints
└── README.md        # Module documentation
```

## Quick Start

### 1. Basic Usage

```hcl
module "vpc" {
  source = "./modules/vpc"
  
  name         = "my-app"
  vpc_cidr     = "10.0.0.0/16"
  cluster_name = "my-eks-cluster"
  
  tags = {
    Environment = "production"
  }
}

module "eks" {
  source = "./modules/eks"
  
  cluster_name       = "my-eks-cluster"
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  
  node_groups = {
    general = {
      desired_size   = 3
      instance_types = ["t3.large"]
    }
  }
}

module "rds" {
  source = "./modules/rds"
  
  name_prefix          = "my-app"
  vpc_id               = module.vpc.vpc_id
  db_subnet_group_name = module.vpc.db_subnet_group_name
  
  databases = {
    order = {
      instance_class = "db.t3.medium"
      multi_az       = true
    }
  }
}
```

### 2. Complete Example

See [examples/complete](../examples/complete) for a full working example that integrates all three modules.

## Design Principles

### 1. **Production-Ready**
- Security best practices by default
- High availability configurations
- Encryption at rest and in transit
- Comprehensive monitoring

### 2. **Flexible & Configurable**
- Sensible defaults for quick starts
- Extensive customization options
- Environment-specific configurations
- Cost optimization options

### 3. **Well-Documented**
- Comprehensive README for each module
- Usage examples
- Input/output documentation
- Architecture diagrams

### 4. **Best Practices**
- Follows AWS Well-Architected Framework
- Infrastructure as Code standards
- Terraform best practices
- Security hardening

## Common Patterns

### Pattern 1: Multi-Environment Deployment

```hcl
# environments/prod/main.tf
module "vpc" {
  source = "../../modules/vpc"
  
  name             = "myapp-prod"
  single_nat_gateway = false  # HA for production
  enable_flow_logs = true
}

# environments/dev/main.tf
module "vpc" {
  source = "../../modules/vpc"
  
  name             = "myapp-dev"
  single_nat_gateway = true   # Cost optimization
  enable_flow_logs = false
}
```

### Pattern 2: Shared VPC, Multiple Clusters

```hcl
module "vpc" {
  source = "./modules/vpc"
  name   = "shared-vpc"
}

module "eks_prod" {
  source             = "./modules/eks"
  cluster_name       = "prod-cluster"
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
}

module "eks_staging" {
  source             = "./modules/eks"
  cluster_name       = "staging-cluster"
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
}
```

### Pattern 3: Database Per Service

```hcl
module "rds" {
  source = "./modules/rds"
  
  databases = {
    users = {
      instance_class = "db.t3.medium"
    }
    orders = {
      instance_class = "db.r6g.large"
    }
    inventory = {
      instance_class = "db.t3.medium"
    }
    analytics = {
      instance_class = "db.r6g.xlarge"
      allocated_storage = 500
    }
  }
}
```

## Module Composition

These modules are designed to work together:

```
┌─────────────────────────────────────────────┐
│                    VPC                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │  Public  │  │ Private  │  │ Database │ │
│  │ Subnets  │  │ Subnets  │  │ Subnets  │ │
│  └──────────┘  └──────────┘  └──────────┘ │
└─────────────────────────────────────────────┘
         │                │              │
         │                │              │
    ┌────▼────┐      ┌────▼─────┐  ┌────▼────┐
    │   ELB   │      │   EKS    │  │   RDS   │
    │ (Public)│      │  Nodes   │  │ (Multi- │
    └─────────┘      │ (Private)│  │   AZ)   │
                     └──────────┘  └─────────┘
```

## Security Considerations

### Network Security
- Private subnets for workloads
- Database subnets isolated from application tier
- Security groups with least privilege
- VPC Flow Logs for audit trail

### Encryption
- KMS encryption for EKS secrets
- KMS encryption for RDS storage
- KMS encryption for EBS volumes
- Secrets Manager for sensitive data

### Access Control
- IRSA for pod-level permissions
- IAM roles instead of access keys
- Private API endpoints supported
- Multi-factor delete protection

## Cost Optimization

### Development Environments

```hcl
# VPC: Use single NAT gateway
single_nat_gateway = true  # Saves ~$64/month

# EKS: Smaller, fewer nodes
node_groups = {
  general = {
    desired_size   = 2
    instance_types = ["t3.medium"]
    capacity_type  = "SPOT"  # Up to 90% savings
  }
}

# RDS: Single-AZ, smaller instances
multi_az                = false
instance_class          = "db.t3.small"
performance_insights_enabled = false
```

### Production Optimizations

```hcl
# Mix spot and on-demand instances
node_groups = {
  on_demand = {
    desired_size   = 2  # Baseline
    capacity_type  = "ON_DEMAND"
  }
  spot = {
    desired_size   = 3  # Burst capacity
    capacity_type  = "SPOT"
  }
}

# RDS: Use Reserved Instances (purchased separately)
# 1-year: 40% savings
# 3-year: 60% savings
```

## Monitoring & Observability

All modules include monitoring capabilities:

- **VPC**: Flow Logs to CloudWatch
- **EKS**: Control plane logs, node metrics
- **RDS**: Enhanced Monitoring, Performance Insights, CloudWatch alarms

## Version Compatibility

| Module | Terraform | AWS Provider |
|--------|-----------|-------------|
| VPC    | >= 1.0    | >= 5.0      |
| EKS    | >= 1.0    | >= 5.0      |
| RDS    | >= 1.0    | >= 5.0      |

## Contributing

When contributing to these modules:

1. Follow existing code structure
2. Update documentation
3. Test in multiple environments
4. Follow semantic versioning
5. Add examples for new features

## Support

### Module-Specific Issues
- See individual module README files
- Check examples directory

### General Questions
- Review AWS documentation
- Check Terraform AWS provider docs

## Resources

- [Terraform AWS Provider Documentation](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [EKS Best Practices Guide](https://aws.github.io/aws-eks-best-practices/)
- [RDS Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)

## License

MIT License - See LICENSE file for details
