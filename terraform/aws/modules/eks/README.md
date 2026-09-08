# EKS Terraform Module

This module creates a production-ready Amazon EKS cluster with managed node groups, IRSA (IAM Roles for Service Accounts), security groups, and essential cluster addons.

## Features

- ✅ EKS cluster with configurable Kubernetes version
- ✅ OIDC provider for IRSA (IAM Roles for Service Accounts)
- ✅ Managed node groups with launch templates
- ✅ KMS encryption for cluster secrets
- ✅ Security groups with least privilege access
- ✅ Essential addons: VPC CNI, CoreDNS, kube-proxy, EBS CSI driver
- ✅ Enhanced monitoring with CloudWatch
- ✅ IMDSv2 enforcement on nodes
- ✅ Encrypted EBS volumes with KMS
- ✅ Support for multiple node groups with taints and labels

## Usage

```hcl
module "eks" {
  source = "./modules/eks"

  cluster_name    = "my-eks-cluster"
  cluster_version = "1.28"

  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  public_subnet_ids  = module.vpc.public_subnet_ids

  cluster_endpoint_public_access_cidrs = ["0.0.0.0/0"]
  
  node_groups = {
    general = {
      desired_size   = 3
      min_size       = 2
      max_size       = 6
      instance_types = ["t3.large"]
      capacity_type  = "ON_DEMAND"
      disk_size      = 50
      labels = {
        role = "general"
      }
    }
    
    spot = {
      desired_size   = 2
      min_size       = 1
      max_size       = 10
      instance_types = ["t3.large", "t3a.large"]
      capacity_type  = "SPOT"
      disk_size      = 50
      labels = {
        role = "spot"
      }
      taints = [
        {
          key    = "spot"
          value  = "true"
          effect = "NoSchedule"
        }
      ]
    }
  }

  tags = {
    Environment = "production"
    Project     = "my-project"
    Terraform   = "true"
  }
}
```

## IRSA (IAM Roles for Service Accounts)

The module creates an OIDC provider that enables IRSA. Example of creating an IAM role for a Kubernetes service account:

```hcl
resource "aws_iam_role" "my_app" {
  name = "my-app-irsa"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = module.eks.oidc_provider_arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${module.eks.oidc_provider_url}:sub" = "system:serviceaccount:default:my-app"
          "${module.eks.oidc_provider_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}
```

## Connecting to the Cluster

After the cluster is created:

```bash
aws eks update-kubeconfig --region <region> --name <cluster-name>
kubectl get nodes
```

## Node Group Configuration

Each node group supports:
- **Scaling**: min_size, desired_size, max_size
- **Instance types**: Single or multiple instance types
- **Capacity type**: ON_DEMAND or SPOT
- **Storage**: Configurable disk size, type (gp3), IOPS, and throughput
- **Labels**: Kubernetes labels for workload scheduling
- **Taints**: Kubernetes taints for dedicated node pools
- **Monitoring**: Enhanced monitoring enabled by default

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| cluster_name | EKS cluster name | string | - | yes |
| cluster_version | Kubernetes version | string | "1.28" | no |
| vpc_id | VPC ID | string | - | yes |
| private_subnet_ids | Private subnet IDs | list(string) | - | yes |
| public_subnet_ids | Public subnet IDs | list(string) | [] | no |
| cluster_endpoint_private_access | Enable private endpoint | bool | true | no |
| cluster_endpoint_public_access | Enable public endpoint | bool | true | no |
| cluster_endpoint_public_access_cidrs | Allowed CIDRs for public access | list(string) | ["0.0.0.0/0"] | no |
| node_groups | Map of node group configurations | map(object) | See variables.tf | no |
| enable_ebs_csi_driver | Enable EBS CSI driver | bool | true | no |
| tags | Additional tags | map(string) | {} | no |

## Outputs

| Name | Description |
|------|-------------|
| cluster_id | EKS cluster name |
| cluster_endpoint | Kubernetes API endpoint |
| cluster_certificate_authority_data | Cluster CA certificate |
| oidc_provider_arn | OIDC provider ARN for IRSA |
| oidc_provider_url | OIDC provider URL |
| node_security_group_id | Node security group ID |
| cluster_security_group_id | Cluster security group ID |
| node_groups | Node group details |

## Security Best Practices

1. **Secrets Encryption**: All secrets encrypted with KMS
2. **IMDSv2**: Instance metadata service v2 enforced
3. **Private API**: Enable private endpoint access
4. **Public Access**: Restrict public CIDR blocks in production
5. **Node Isolation**: Use security groups and network policies
6. **Least Privilege**: IAM roles follow least privilege principle

## Cluster Addons

- **VPC CNI**: Amazon VPC networking for pods
- **CoreDNS**: Cluster DNS service
- **kube-proxy**: Network proxy
- **EBS CSI Driver**: Persistent volume support with IRSA

## Requirements

| Name | Version |
|------|---------|  
| terraform | >= 1.0 |
| aws | >= 5.0 |
| tls | >= 4.0 |
