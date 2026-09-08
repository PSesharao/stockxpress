# VPC Terraform Module

This module creates a highly available VPC with public, private, and database subnets across multiple Availability Zones, optimized for EKS workloads.

## Features

- ✅ VPC with configurable CIDR block
- ✅ Public, private, and database subnets across 3 AZs (configurable)
- ✅ Internet Gateway for public subnets
- ✅ NAT Gateways for private subnets (supports single or per-AZ)
- ✅ Route tables for each subnet type
- ✅ Proper EKS tags for subnet discovery
- ✅ DB subnet group for RDS
- ✅ VPC Flow Logs (optional)
- ✅ DNS support and hostnames enabled

## Usage

```hcl
module "vpc" {
  source = "./modules/vpc"

  name         = "my-app"
  vpc_cidr     = "10.0.0.0/16"
  az_count     = 3
  cluster_name = "my-eks-cluster"

  enable_nat_gateway = true
  single_nat_gateway = false  # Use one NAT per AZ for HA
  enable_flow_logs   = true

  tags = {
    Environment = "production"
    Project     = "my-project"
    Terraform   = "true"
  }
}
```

## Subnet CIDR Allocation

The module automatically calculates subnet CIDRs using the following pattern:
- **Public subnets**: `cidrsubnet(vpc_cidr, 8, 0-2)` → 10.0.0.0/24, 10.0.1.0/24, 10.0.2.0/24
- **Private subnets**: `cidrsubnet(vpc_cidr, 8, 3-5)` → 10.0.3.0/24, 10.0.4.0/24, 10.0.5.0/24
- **Database subnets**: `cidrsubnet(vpc_cidr, 8, 6-8)` → 10.0.6.0/24, 10.0.7.0/24, 10.0.8.0/24

## EKS Integration

The module automatically tags subnets for EKS:
- **Public subnets**: `kubernetes.io/role/elb = 1` (for public load balancers)
- **Private subnets**: `kubernetes.io/role/internal-elb = 1` (for internal load balancers)
- **All subnets**: `kubernetes.io/cluster/<cluster-name> = shared`

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| name | Name prefix for VPC resources | string | - | yes |
| vpc_cidr | CIDR block for VPC | string | "10.0.0.0/16" | no |
| az_count | Number of Availability Zones (2-6) | number | 3 | no |
| cluster_name | EKS cluster name for tagging | string | - | yes |
| enable_nat_gateway | Enable NAT Gateway | bool | true | no |
| single_nat_gateway | Use single NAT Gateway (cost optimization) | bool | false | no |
| enable_flow_logs | Enable VPC Flow Logs | bool | false | no |
| flow_logs_retention_days | Flow logs retention period | number | 7 | no |
| tags | Additional tags | map(string) | {} | no |

## Outputs

| Name | Description |
|------|-------------|
| vpc_id | VPC ID |
| vpc_cidr | VPC CIDR block |
| public_subnet_ids | List of public subnet IDs |
| private_subnet_ids | List of private subnet IDs |
| database_subnet_ids | List of database subnet IDs |
| nat_gateway_ids | List of NAT Gateway IDs |
| nat_public_ips | List of NAT Gateway public IPs |
| db_subnet_group_name | DB subnet group name |
| availability_zones | List of AZs used |

## Cost Optimization

For non-production environments, consider:
- Setting `single_nat_gateway = true` (saves ~$96/month for 3 AZs)
- Reducing `az_count` to 2
- Disabling VPC Flow Logs

## High Availability

For production environments:
- Use `single_nat_gateway = false` for NAT Gateway redundancy
- Keep `az_count = 3` for maximum availability
- Enable VPC Flow Logs for troubleshooting

## Requirements

| Name | Version |
|------|---------|  
| terraform | >= 1.0 |
| aws | >= 5.0 |
