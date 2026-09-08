# AWS Secrets Manager Terraform Module

This module manages sensitive credentials and configuration data for the StockXpress application using AWS Secrets Manager.

## Features

- **Centralized Secret Management**: Single source of truth for all credentials
- **Encryption**: KMS encryption for all secrets at rest
- **Rotation Support**: IAM roles for automatic password rotation
- **Version Control**: Automatic versioning of secret values
- **Access Control**: IAM and resource policies for fine-grained access
- **Recovery**: Configurable recovery window for deleted secrets
- **Integration**: Seamless integration with EKS via External Secrets Operator

## Secrets Managed

1. **RDS Order Database**: MySQL credentials for Order Service
2. **RDS Inventory Database**: MySQL credentials for Inventory Service
3. **DocumentDB**: MongoDB credentials for Product Service
4. **ElastiCache Redis**: Auth token for caching
5. **JWT Secret**: Signing key for API Gateway
6. **Keycloak Admin**: Admin credentials for Keycloak
7. **Email SMTP**: SMTP credentials for notifications

## Usage

### Basic Configuration

```hcl
module "secrets_manager" {
  source = "./modules/secrets"
  
  project_name = "stockxpress"
  environment  = "production"
  
  # RDS credentials
  rds_order_password     = module.rds.order_db_password
  rds_order_endpoint     = module.rds.order_db_endpoint
  rds_inventory_password = module.rds.inventory_db_password
  rds_inventory_endpoint = module.rds.inventory_db_endpoint
  
  # DocumentDB credentials
  documentdb_password = module.documentdb.master_password
  documentdb_endpoint = module.documentdb.cluster_endpoint
  
  # ElastiCache credentials
  elasticache_auth_token = module.elasticache.auth_token
  elasticache_endpoint   = module.elasticache.primary_endpoint_address
  
  # Additional secrets
  create_jwt_secret      = true
  create_keycloak_secret = true
  create_email_secret    = false
  
  tags = {
    Project     = "StockXpress"
    Environment = "production"
    ManagedBy   = "Terraform"
  }
}
```

### With Email Configuration

```hcl
module "secrets_manager" {
  source = "./modules/secrets"
  
  project_name = "stockxpress"
  environment  = "production"
  
  # ... other configurations ...
  
  # Email SMTP configuration
  create_email_secret = true
  smtp_host          = "smtp.gmail.com"
  smtp_port          = 587
  smtp_username      = "noreply@stockxpress.com"
  smtp_password      = var.smtp_password
  
  tags = local.common_tags
}
```

### With EKS Integration

```hcl
module "secrets_manager" {
  source = "./modules/secrets"
  
  project_name = "stockxpress"
  environment  = "production"
  
  # ... database credentials ...
  
  # Allow EKS service accounts to read secrets
  enable_resource_policy = true
  eks_service_account_roles = [
    module.eks.order_service_role_arn,
    module.eks.inventory_service_role_arn,
    module.eks.product_service_role_arn
  ]
  
  tags = local.common_tags
}
```

## Secret Structure

### Database Secrets (RDS/DocumentDB)

```json
{
  "username": "orderadmin",
  "password": "<generated-password>",
  "engine": "mysql",
  "host": "stockxpress-prod-order.abc123.us-east-1.rds.amazonaws.com",
  "port": 3306,
  "dbname": "order_service"
}
```

### Redis Secret

```json
{
  "auth_token": "<generated-token>",
  "endpoint": "stockxpress-prod-redis.abc123.cache.amazonaws.com",
  "port": 6379
}
```

### JWT Secret

```json
{
  "secret": "<64-character-random-string>"
}
```

## Spring Boot Integration

### Using AWS SDK

#### Dependencies (pom.xml)

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>secretsmanager</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

#### Configuration Class

```java
@Configuration
public class SecretsManagerConfig {
    
    @Value("${aws.secrets.database.name}")
    private String databaseSecretName;
    
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
            .region(Region.US_EAST_1)
            .build();
    }
    
    @Bean
    public DatabaseCredentials databaseCredentials(SecretsManagerClient client) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId(databaseSecretName)
            .build();
        
        GetSecretValueResponse response = client.getSecretValue(request);
        String secretString = response.secretString();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(secretString, DatabaseCredentials.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse secret", e);
        }
    }
}

@Data
public class DatabaseCredentials {
    private String username;
    private String password;
    private String host;
    private int port;
    private String dbname;
}
```

#### DataSource Configuration

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource(DatabaseCredentials credentials) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(
            "jdbc:mysql://%s:%d/%s",
            credentials.getHost(),
            credentials.getPort(),
            credentials.getDbname()
        ));
        config.setUsername(credentials.getUsername());
        config.setPassword(credentials.getPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        
        return new HikariDataSource(config);
    }
}
```

### Using Spring Cloud AWS

#### Dependencies

```xml
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-secrets-manager-config</artifactId>
</dependency>
```

#### Configuration (application.yml)

```yaml
aws:
  secretsmanager:
    region: us-east-1
    prefix: /config
    default-context: application
    profile-separator: '_'

spring:
  config:
    import: aws-secretsmanager:stockxpress/production/rds/order
  
  datasource:
    url: jdbc:mysql://${host}:${port}/${dbname}
    username: ${username}
    password: ${password}
```

## Kubernetes Integration with External Secrets Operator

### ExternalSecret Resource

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: order-service-db-secret
  namespace: default
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secretsmanager
    kind: SecretStore
  target:
    name: order-service-db-credentials
    creationPolicy: Owner
  data:
  - secretKey: username
    remoteRef:
      key: stockxpress/production/rds/order
      property: username
  - secretKey: password
    remoteRef:
      key: stockxpress/production/rds/order
      property: password
  - secretKey: database-url
    remoteRef:
      key: stockxpress/production/rds/order
      property: host
```

### SecretStore Configuration

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secretsmanager
  namespace: default
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa
```

### Using Secret in Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  template:
    spec:
      containers:
      - name: order-service
        image: stockxpress/order-service:v1.0.0
        env:
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: order-service-db-credentials
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: order-service-db-credentials
              key: password
```

## AWS CLI Commands

### Retrieve Secret

```bash
# Get secret value
aws secretsmanager get-secret-value \
  --secret-id stockxpress/production/rds/order \
  --query SecretString \
  --output text | jq .

# Get specific field
aws secretsmanager get-secret-value \
  --secret-id stockxpress/production/rds/order \
  --query SecretString \
  --output text | jq -r '.password'
```

### Update Secret

```bash
# Update secret value
aws secretsmanager update-secret \
  --secret-id stockxpress/production/rds/order \
  --secret-string '{"username":"orderadmin","password":"new-password","host":"...","port":3306,"dbname":"order_service"}'

# Update description
aws secretsmanager update-secret \
  --secret-id stockxpress/production/rds/order \
  --description "Updated Order Service RDS credentials"
```

### Rotate Secret

```bash
# Trigger rotation
aws secretsmanager rotate-secret \
  --secret-id stockxpress/production/rds/order \
  --rotation-lambda-arn arn:aws:lambda:us-east-1:123456789012:function:SecretsManagerRotation

# Check rotation status
aws secretsmanager describe-secret \
  --secret-id stockxpress/production/rds/order \
  --query 'RotationEnabled'
```

### List Secrets

```bash
# List all secrets
aws secretsmanager list-secrets \
  --filters Key=name,Values=stockxpress/production/

# List with tags
aws secretsmanager list-secrets \
  --filters Key=tag-key,Values=Environment Key=tag-value,Values=production
```

## IAM Policies

### Application Read Policy

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
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:123456789012:secret:stockxpress/production/rds/order-*",
        "arn:aws:secretsmanager:us-east-1:123456789012:secret:stockxpress/production/jwt/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt",
        "kms:DescribeKey"
      ],
      "Resource": "arn:aws:kms:us-east-1:123456789012:key/<kms-key-id>"
    }
  ]
}
```

### Rotation Lambda Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:DescribeSecret",
        "secretsmanager:GetSecretValue",
        "secretsmanager:PutSecretValue",
        "secretsmanager:UpdateSecretVersionStage"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:stockxpress/*/*"
    },
    {
      "Effect": "Allow",
      "Action": "secretsmanager:GetRandomPassword",
      "Resource": "*"
    }
  ]
}
```

## Secret Rotation

### Automatic Rotation with Lambda

```python
# Lambda function for RDS password rotation
import boto3
import json
import pymysql

def lambda_handler(event, context):
    secret_id = event['SecretId']
    token = event['ClientRequestToken']
    step = event['Step']
    
    secrets_client = boto3.client('secretsmanager')
    
    if step == 'createSecret':
        # Generate new password
        current_secret = secrets_client.get_secret_value(SecretId=secret_id)
        current_dict = json.loads(current_secret['SecretString'])
        
        new_password = secrets_client.get_random_password(
            PasswordLength=32,
            ExcludeCharacters='"@/\\'
        )['RandomPassword']
        
        current_dict['password'] = new_password
        
        # Store new version with AWSPENDING label
        secrets_client.put_secret_value(
            SecretId=secret_id,
            ClientRequestToken=token,
            SecretString=json.dumps(current_dict),
            VersionStages=['AWSPENDING']
        )
    
    elif step == 'setSecret':
        # Update database with new password
        pending_secret = secrets_client.get_secret_value(
            SecretId=secret_id,
            VersionStage='AWSPENDING'
        )
        pending_dict = json.loads(pending_secret['SecretString'])
        
        # Connect and update password
        connection = pymysql.connect(
            host=pending_dict['host'],
            user=pending_dict['username'],
            password=pending_dict['password']
        )
        
        with connection.cursor() as cursor:
            cursor.execute(
                f"ALTER USER '{pending_dict['username']}'@'%' IDENTIFIED BY '{pending_dict['password']}'"
            )
        connection.commit()
        connection.close()
    
    elif step == 'testSecret':
        # Test new credentials
        pending_secret = secrets_client.get_secret_value(
            SecretId=secret_id,
            VersionStage='AWSPENDING'
        )
        pending_dict = json.loads(pending_secret['SecretString'])
        
        # Test connection
        connection = pymysql.connect(
            host=pending_dict['host'],
            user=pending_dict['username'],
            password=pending_dict['password'],
            database=pending_dict['dbname']
        )
        connection.close()
    
    elif step == 'finishSecret':
        # Move AWSCURRENT label to new version
        secrets_client.update_secret_version_stage(
            SecretId=secret_id,
            VersionStage='AWSCURRENT',
            MoveToVersionId=token
        )
    
    return {'statusCode': 200}
```

## Best Practices

1. **Separation**: Use separate secrets for each environment and service
2. **Least Privilege**: Grant minimal required permissions to access secrets
3. **Rotation**: Implement automatic rotation for database credentials
4. **Versioning**: Use version labels to manage secret updates
5. **Encryption**: Always use KMS for encryption at rest
6. **Recovery**: Set appropriate recovery window (30 days recommended)
7. **Monitoring**: Set up CloudWatch alarms for unauthorized access attempts
8. **Audit**: Enable CloudTrail logging for all Secrets Manager API calls

## Cost Considerations

- **Storage**: $0.40 per secret per month
- **API Calls**: $0.05 per 10,000 API calls
- **Rotation**: Lambda execution costs for rotation functions

### Cost Optimization:
1. Combine related credentials in single secret (e.g., DB username + password)
2. Cache secret values in application to reduce API calls
3. Use resource policies to limit access instead of separate secrets

## Monitoring

### CloudWatch Metrics

```bash
# Create alarm for failed secret retrievals
aws cloudwatch put-metric-alarm \
  --alarm-name stockxpress-secrets-access-errors \
  --alarm-description "Alert on failed secret access attempts" \
  --metric-name GetSecretValue \
  --namespace AWS/SecretsManager \
  --statistic Sum \
  --period 300 \
  --threshold 5 \
  --comparison-operator GreaterThanThreshold
```

### CloudTrail Events

Monitor these events:
- `GetSecretValue`: Secret access
- `PutSecretValue`: Secret updates
- `DeleteSecret`: Secret deletions
- `RotateSecret`: Rotation triggers

## Troubleshooting

### Access Denied

```bash
# Check IAM permissions
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::123456789012:role/OrderServiceRole \
  --action-names secretsmanager:GetSecretValue \
  --resource-arns arn:aws:secretsmanager:us-east-1:123456789012:secret:stockxpress/production/rds/order-*

# Verify KMS permissions
aws kms describe-key --key-id <kms-key-id>
```

### Secret Not Found

```bash
# List secrets with prefix
aws secretsmanager list-secrets \
  --filters Key=name,Values=stockxpress/production/

# Check secret details
aws secretsmanager describe-secret \
  --secret-id stockxpress/production/rds/order
```

### Rotation Failures

```bash
# Check rotation configuration
aws secretsmanager describe-secret \
  --secret-id stockxpress/production/rds/order \
  --query 'RotationRules'

# View Lambda logs
aws logs tail /aws/lambda/SecretsManagerRotation --follow
```

## Migration Guide

### From Environment Variables

**Before:**
```yaml
env:
  - name: DB_PASSWORD
    value: "hardcoded-password"
```

**After:**
```yaml
env:
  - name: AWS_SECRET_NAME
    value: "stockxpress/production/rds/order"
```

### From ConfigMaps

**Before:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: db-config
data:
  password: "base64-encoded-password"
```

**After:**
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: db-secret
spec:
  secretStoreRef:
    name: aws-secretsmanager
  data:
  - secretKey: password
    remoteRef:
      key: stockxpress/production/rds/order
      property: password
```

## References

- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/)
- [Secrets Manager Rotation](https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets.html)
- [External Secrets Operator](https://external-secrets.io/)
- [Spring Cloud AWS](https://awspring.io/)
