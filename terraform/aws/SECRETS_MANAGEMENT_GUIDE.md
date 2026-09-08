# StockXpress Terraform - Secret Management Guide

## Overview

This guide explains how to properly manage sensitive data (passwords, tokens, API keys) when deploying StockXpress infrastructure with Terraform.

**CRITICAL SECURITY RULE:**  
**NEVER commit passwords, tokens, or any sensitive data to version control.**

All secrets MUST be managed externally using AWS Secrets Manager and referenced in Terraform via data sources.

---

## Prerequisites

1. **AWS CLI** configured with appropriate credentials
2. **AWS Secrets Manager** permissions: `secretsmanager:CreateSecret`, `secretsmanager:GetSecretValue`
3. **Terraform** >= 1.5.0

---

## Step 1: Create Secrets in AWS Secrets Manager

### Development Environment

```bash
# RDS Order Service Password
aws secretsmanager create-secret \
  --name "stockxpress/dev/rds-order-password" \
  --description "RDS master password for Order Service (dev)" \
  --secret-string "$(openssl rand -base64 32)" \
  --region us-east-1

# RDS Inventory Service Password
aws secretsmanager create-secret \
  --name "stockxpress/dev/rds-inventory-password" \
  --description "RDS master password for Inventory Service (dev)" \
  --secret-string "$(openssl rand -base64 32)" \
  --region us-east-1

# DocumentDB Password
aws secretsmanager create-secret \
  --name "stockxpress/dev/documentdb-password" \
  --description "DocumentDB master password for Product Service (dev)" \
  --secret-string "$(openssl rand -base64 32)" \
  --region us-east-1

# Redis Auth Token
aws secretsmanager create-secret \
  --name "stockxpress/dev/redis-auth-token" \
  --description "ElastiCache Redis AUTH token (dev)" \
  --secret-string "$(openssl rand -base64 32)" \
  --region us-east-1
```

### Production Environment

```bash
# Production secrets with additional security controls
aws secretsmanager create-secret \
  --name "stockxpress/production/rds-order-password" \
  --description "RDS master password for Order Service (production)" \
  --secret-string "$(openssl rand -base64 32)" \
  --kms-key-id "alias/stockxpress-production-secrets" \
  --tags Key=Environment,Value=production Key=ManagedBy,Value=Terraform \
  --region us-east-1

# Enable automatic rotation (requires Lambda function)
aws secretsmanager rotate-secret \
  --secret-id "stockxpress/production/rds-order-password" \
  --rotation-lambda-arn "arn:aws:lambda:us-east-1:ACCOUNT_ID:function:stockxpress-secret-rotator" \
  --rotation-rules AutomaticallyAfterDays=90
```

---

## Step 2: Reference Secrets in Terraform

### Create Data Sources (main.tf)

```hcl
# Retrieve secrets from AWS Secrets Manager
data "aws_secretsmanager_secret_version" "rds_order_password" {
  secret_id = "stockxpress/${var.environment}/rds-order-password"
}

data "aws_secretsmanager_secret_version" "rds_inventory_password" {
  secret_id = "stockxpress/${var.environment}/rds-inventory-password"
}

data "aws_secretsmanager_secret_version" "documentdb_password" {
  secret_id = "stockxpress/${var.environment}/documentdb-password"
}

data "aws_secretsmanager_secret_version" "redis_auth_token" {
  secret_id = "stockxpress/${var.environment}/redis-auth-token"
}
```

### Pass to Modules

```hcl
# RDS Module
module "rds" {
  source = "./modules/rds"
  
  # ... other configuration ...
  
  databases = {
    order = {
      instance_class  = "db.t3.medium"
      database_name   = "order_service"
      username        = "orderadmin"
      # SECURE: Password retrieved from AWS Secrets Manager
      master_password = data.aws_secretsmanager_secret_version.rds_order_password.secret_string
      multi_az        = var.environment == "production"
    }
    inventory = {
      instance_class  = "db.t3.medium"
      database_name   = "inventory_service"
      username        = "inventoryadmin"
      # SECURE: Password retrieved from AWS Secrets Manager
      master_password = data.aws_secretsmanager_secret_version.rds_inventory_password.secret_string
      multi_az        = var.environment == "production"
    }
  }
}

# DocumentDB Module
module "documentdb" {
  source = "./modules/documentdb"
  
  # ... other configuration ...
  
  master_username = "productadmin"
  # SECURE: Password retrieved from AWS Secrets Manager
  master_password = data.aws_secretsmanager_secret_version.documentdb_password.secret_string
}

# ElastiCache Module
module "elasticache" {
  source = "./modules/elasticache"
  
  # ... other configuration ...
  
  auth_token_enabled = true
  # SECURE: Auth token retrieved from AWS Secrets Manager
  redis_auth_token   = data.aws_secretsmanager_secret_version.redis_auth_token.secret_string
}
```

---

## Step 3: Deploy with Terraform

```bash
# Initialize Terraform
cd terraform/aws
terraform init

# Validate configuration
terraform validate

# Plan deployment (verify secrets are NOT shown in plan output)
terraform plan -var-file=environments/dev.tfvars

# Apply (secrets will be retrieved at runtime)
terraform apply -var-file=environments/dev.tfvars
```

---

## Step 4: Application Configuration

Applications should also retrieve secrets from AWS Secrets Manager, NOT from Terraform outputs.

### Using Kubernetes External Secrets Operator

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: order-service-db-credentials
  namespace: stockxpress
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: order-service-db-secret
    creationPolicy: Owner
  data:
    - secretKey: password
      remoteRef:
        key: stockxpress/production/rds-order-password
    - secretKey: username
      remoteRef:
        key: stockxpress/production/rds-order-username
```

### Using AWS SDK in Application Code (Java)

```java
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class DatabaseConfig {
    
    private final SecretsManagerClient secretsClient;
    
    public String getDatabasePassword() {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId("stockxpress/production/rds-order-password")
            .build();
        
        return secretsClient.getSecretValue(request).secretString();
    }
}
```

---

## Security Best Practices

### ✅ DO

1. **Store ALL secrets in AWS Secrets Manager**
   - Database passwords
   - Redis auth tokens
   - API keys
   - JWT signing keys
   - TLS certificates

2. **Use strong, randomly generated secrets**
   ```bash
   openssl rand -base64 32  # For passwords
   openssl rand -base64 64  # For signing keys
   ```

3. **Enable secret rotation**
   - Development: Every 90 days
   - Production: Every 60-90 days with Lambda rotation function

4. **Tag secrets for organization**
   ```bash
   --tags Key=Environment,Value=production Key=Service,Value=order-service
   ```

5. **Enable deletion protection in production**
   ```bash
   aws secretsmanager update-secret \
     --secret-id stockxpress/production/rds-order-password \
     --description "PROTECTED - Production database password"
   ```

6. **Use KMS encryption for production secrets**
   ```bash
   --kms-key-id alias/stockxpress-production-secrets
   ```

7. **Limit IAM permissions**
   - Only Terraform execution role can read secrets
   - Only administrators can update/rotate secrets
   - Applications use IRSA (IAM Roles for Service Accounts)

### ❌ DO NOT

1. **Never commit secrets to Git**
   - ❌ No passwords in `.tfvars` files
   - ❌ No tokens in Terraform modules
   - ❌ No API keys in configuration files
   - ❌ No secrets in environment variables committed to repo

2. **Never output secrets from Terraform modules**
   - ❌ No `output "password"` blocks
   - ❌ No connection strings with embedded passwords
   - Even with `sensitive = true`, avoid exposing secrets

3. **Never share secrets via insecure channels**
   - ❌ No Slack/email/chat messages
   - ❌ No shared documents
   - ✅ Use AWS Secrets Manager sharing or temporary IAM credentials

4. **Never reuse secrets across environments**
   - Dev, staging, and production must have separate secrets
   - Different AWS accounts = different secrets

---

## Secret Rotation Procedures

### Manual Rotation (Development)

```bash
# 1. Generate new password
NEW_PASSWORD=$(openssl rand -base64 32)

# 2. Update secret in AWS Secrets Manager
aws secretsmanager update-secret \
  --secret-id "stockxpress/dev/rds-order-password" \
  --secret-string "$NEW_PASSWORD"

# 3. Update RDS master password
aws rds modify-db-instance \
  --db-instance-identifier stockxpress-dev-order \
  --master-user-password "$NEW_PASSWORD" \
  --apply-immediately

# 4. Restart application pods to pick up new password
kubectl rollout restart deployment/order-service -n stockxpress
```

### Automatic Rotation (Production)

Use AWS Lambda with Secrets Manager rotation:

1. **Create rotation Lambda function**
2. **Configure rotation schedule** (60-90 days)
3. **Lambda updates both Secrets Manager AND database**
4. **Applications automatically pick up new credentials**

See: https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets.html

---

## Troubleshooting

### Error: "Secret not found"

```
Error: error reading Secrets Manager Secret Version: ResourceNotFoundException
```

**Solution:** Create the secret before running Terraform:

```bash
aws secretsmanager create-secret \
  --name "stockxpress/dev/rds-order-password" \
  --secret-string "$(openssl rand -base64 32)"
```

### Error: "Access Denied"

```
Error: AccessDeniedException: User is not authorized to perform: secretsmanager:GetSecretValue
```

**Solution:** Add IAM permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:stockxpress/*"
    }
  ]
}
```

### How to Retrieve Secrets for Local Development

```bash
# Get database password
aws secretsmanager get-secret-value \
  --secret-id "stockxpress/dev/rds-order-password" \
  --query SecretString \
  --output text

# Use in connection string
export DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id "stockxpress/dev/rds-order-password" \
  --query SecretString --output text)

mysql -h stockxpress-dev-order.xxx.us-east-1.rds.amazonaws.com \
  -u orderadmin -p"$DB_PASSWORD" order_service
```

---

## Compliance and Auditing

### CloudTrail Logging

All secret access is logged in AWS CloudTrail:

```bash
# View secret access history
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=ResourceName,AttributeValue=stockxpress/production/rds-order-password \
  --max-results 50
```

### Secrets Manager Audit Report

```bash
# List all secrets and their rotation status
aws secretsmanager list-secrets \
  --filters Key=name,Values=stockxpress/ \
  --query 'SecretList[].{Name:Name,Rotation:RotationEnabled,LastRotated:LastRotatedDate}' \
  --output table
```

---

## Emergency Procedures

### Secret Compromised

If a secret is exposed (e.g., committed to Git):

1. **Immediately rotate the secret**
   ```bash
   aws secretsmanager rotate-secret \
     --secret-id "stockxpress/production/rds-order-password" \
     --rotate-immediately
   ```

2. **Update database password**
3. **Restart all application pods**
4. **Review CloudTrail logs for unauthorized access**
5. **File security incident report**
6. **Remove from Git history** (BFG Repo-Cleaner or git-filter-repo)

---

## Summary

✅ **Secrets are managed externally in AWS Secrets Manager**  
✅ **Terraform retrieves secrets via data sources at runtime**  
✅ **No secrets are committed to version control**  
✅ **Applications retrieve secrets from AWS Secrets Manager**  
✅ **Secrets are rotated regularly (60-90 days)**  
✅ **All secret access is audited via CloudTrail**  

This approach ensures StockXpress meets Advanced certification security standards.
