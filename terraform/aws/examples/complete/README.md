# Complete AWS Infrastructure Example

This example demonstrates how to deploy a complete AWS infrastructure stack using the VPC, EKS, and RDS modules.

## Architecture

The infrastructure includes:

- **VPC**: Multi-AZ VPC with public, private, and database subnets
- **EKS**: Kubernetes cluster with managed node groups
- **RDS**: Two MySQL databases (order and inventory) with Multi-AZ

## Prerequisites

1. **AWS Account**: Active AWS account with appropriate permissions
2. **AWS CLI**: Configured with credentials (`aws configure`)
3. **Terraform**: Version >= 1.0 installed
4. **kubectl**: For interacting with the EKS cluster

## Quick Start

### 1. Configure Variables

```bash
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values
```

### 2. Initialize Terraform

```bash
terraform init
```

### 3. Review Plan

```bash
terraform plan
```

### 4. Apply Configuration

```bash
terraform apply
```

This will create:
- 1 VPC with 9 subnets (3 public, 3 private, 3 database)
- 3 NAT Gateways (or 1 if single_nat_gateway = true)
- 1 EKS cluster with 2 node groups
- 2 RDS MySQL instances
- Security groups, IAM roles, KMS keys, etc.

**Estimated time**: 15-20 minutes

## Post-Deployment

### Configure kubectl

```bash
aws eks update-kubeconfig --region us-east-1 --name stockxpress-production-eks

# Verify connection
kubectl get nodes
kubectl get pods -A
```

### Retrieve Database Credentials

```bash
# Get secret ARNs
terraform output rds_order_secret_arn
terraform output rds_inventory_secret_arn

# Retrieve credentials
aws secretsmanager get-secret-value \
  --secret-id $(terraform output -raw rds_order_secret_arn) \
  --query SecretString \
  --output text | jq .
```

### Test Database Connection

```bash
# From bastion host or EKS pod
mysql -h <order-db-endpoint> -u admin -p
```

## Environment-Specific Configurations

### Development

```hcl
environment        = "dev"
single_nat_gateway = true   # Cost savings
az_count           = 2      # Minimum HA

eks_node_groups = {
  general = {
    desired_size   = 2
    min_size       = 1
    max_size       = 3
    instance_types = ["t3.medium"]
  }
}

rds_multi_az                = false
rds_deletion_protection     = false
rds_backup_retention_period = 3
```

### Staging

```hcl
environment        = "staging"
single_nat_gateway = false
az_count           = 3

eks_node_groups = {
  general = {
    desired_size   = 2
    instance_types = ["t3.large"]
  }
}

rds_multi_az                = true
rds_deletion_protection     = true
rds_backup_retention_period = 7
```

### Production

```hcl
environment        = "production"
single_nat_gateway = false
az_count           = 3

eks_node_groups = {
  general = {
    desired_size   = 3
    min_size       = 2
    max_size       = 10
    instance_types = ["t3.xlarge"]
  }
  spot = {
    desired_size   = 3
    instance_types = ["t3.large"]
    capacity_type  = "SPOT"
  }
}

rds_multi_az                = true
rds_deletion_protection     = true
rds_backup_retention_period = 14

# Restrict public access
eks_endpoint_public_access_cidrs = ["1.2.3.4/32"]
```

## Cost Estimation

### Production Configuration (us-east-1)

| Resource | Quantity | Monthly Cost |
|----------|----------|-------------|
| NAT Gateway | 3 | $98.55 |
| EKS Cluster | 1 | $73.00 |
| EKS Nodes (t3.large) | 5 | $304.80 |
| RDS (db.r6g.xlarge) | 1 | $435.00 |
| RDS (db.r6g.large) | 1 | $217.50 |
| RDS Storage (300GB gp3) | - | $36.00 |
| **Total** | | **~$1,164.85** |

*Does not include data transfer, CloudWatch, or Secrets Manager costs*

### Development Configuration

| Resource | Quantity | Monthly Cost |
|----------|----------|-------------|
| NAT Gateway | 1 | $32.85 |
| EKS Cluster | 1 | $73.00 |
| EKS Nodes (t3.medium) | 2 | $60.96 |
| RDS (db.t3.medium) | 2 | $126.72 |
| RDS Storage (200GB gp3) | - | $24.00 |
| **Total** | | **~$317.53** |

## Outputs

```bash
# View all outputs
terraform output

# Specific outputs
terraform output vpc_id
terraform output eks_cluster_endpoint
terraform output rds_order_endpoint
```

## Accessing Resources

### EKS Cluster

```bash
# Update kubeconfig
aws eks update-kubeconfig --region us-east-1 --name stockxpress-production-eks

# Deploy sample application
kubectl apply -f https://k8s.io/examples/application/deployment.yaml
```

### RDS Databases

Create Kubernetes secrets from Secrets Manager:

```bash
# Install External Secrets Operator (recommended)
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace

# Create ExternalSecret
cat <<EOF | kubectl apply -f -
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: order-db-credentials
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: order-db-secret
  data:
  - secretKey: username
    remoteRef:
      key: $(terraform output -raw rds_order_secret_arn)
      property: username
  - secretKey: password
    remoteRef:
      key: $(terraform output -raw rds_order_secret_arn)
      property: password
  - secretKey: host
    remoteRef:
      key: $(terraform output -raw rds_order_secret_arn)
      property: host
EOF
```

## Monitoring

### CloudWatch Dashboards

Create custom dashboards for monitoring:
- EKS cluster metrics
- RDS performance metrics
- VPC flow logs

### CloudWatch Alarms

The configuration includes alarms for:
- RDS CPU utilization > 80%
- RDS free storage < 10GB
- RDS freeable memory < 1GB

## Backup and Disaster Recovery

### RDS Backups

- **Automated backups**: Daily during backup window (03:00-04:00 UTC)
- **Retention**: 14 days (production), 7 days (staging), 3 days (dev)
- **Manual snapshots**: Create before major changes

```bash
aws rds create-db-snapshot \
  --db-instance-identifier stockxpress-production-order \
  --db-snapshot-identifier stockxpress-order-pre-migration-$(date +%Y%m%d)
```

### EKS Backups

Consider using:
- **Velero**: For cluster and application backup
- **EBS snapshots**: For persistent volume backups

## Security Best Practices

1. **Network Isolation**
   - Databases in private subnets
   - No public access to RDS instances
   - Security groups with least privilege

2. **Encryption**
   - EKS secrets encrypted with KMS
   - RDS storage encrypted with KMS
   - EBS volumes encrypted

3. **Access Control**
   - IRSA for pod-level IAM permissions
   - Secrets Manager for credentials
   - IAM roles instead of access keys

4. **Monitoring**
   - VPC Flow Logs enabled
   - EKS control plane logging
   - RDS Enhanced Monitoring

## Cleanup

To destroy all resources:

```bash
# Disable deletion protection first
terraform apply -var="rds_deletion_protection=false"

# Destroy infrastructure
terraform destroy
```

**Warning**: This will permanently delete all resources including databases. Ensure backups are taken if needed.

## Troubleshooting

### EKS Cluster Creation Timeout

- Check VPC and subnet configuration
- Verify IAM roles have correct permissions
- Review CloudWatch logs for control plane

### RDS Connection Issues

- Verify security group rules
- Check subnet group configuration
- Ensure correct database endpoint
- Test from EKS pod in same VPC

### Node Group Scaling Issues

- Check service quotas for EC2 instances
- Review autoscaler logs
- Verify IAM permissions

## Support

For issues or questions:
1. Check module README files
2. Review AWS documentation
3. Check Terraform AWS provider docs

## License

MIT License - see LICENSE file for details
