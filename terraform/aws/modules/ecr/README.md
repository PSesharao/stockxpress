# ECR (Elastic Container Registry) Terraform Module

This module creates AWS ECR repositories for storing Docker images of StockXpress microservices.

## Features

- **Private Repositories**: Secure Docker image storage
- **Image Scanning**: Automated vulnerability scanning on push
- **Lifecycle Management**: Automatic cleanup of old images
- **Encryption**: KMS encryption for images at rest
- **Replication**: Multi-region replication support
- **Cross-Account Access**: Share images across AWS accounts
- **Immutability**: Optional image tag immutability

## Usage

### Basic Configuration

```hcl
module "ecr" {
  source = "./modules/ecr"
  
  project_name = "stockxpress"
  environment  = "production"
  
  repositories = [
    "discovery-server",
    "api-gateway",
    "product-service",
    "inventory-service",
    "order-service",
    "notification-service"
  ]
  
  scan_on_push         = true
  image_tag_mutability = "MUTABLE"
  
  tags = {
    Project     = "StockXpress"
    Environment = "production"
    ManagedBy   = "Terraform"
  }
}
```

### Advanced Configuration

```hcl
module "ecr" {
  source = "./modules/ecr"
  
  project_name = "stockxpress"
  environment  = "production"
  
  repositories = [
    "discovery-server",
    "api-gateway",
    "product-service",
    "inventory-service",
    "order-service",
    "notification-service"
  ]
  
  # Image settings
  image_tag_mutability = "IMMUTABLE"  # Prevent tag overwrites
  scan_on_push         = true
  
  # Enhanced scanning with AWS Inspector
  enable_enhanced_scanning = true
  enhanced_scan_frequency  = "CONTINUOUS_SCAN"
  
  # Lifecycle policy
  max_image_count             = 50
  untagged_image_days         = 7
  keep_production_images      = true
  max_production_image_count  = 200
  
  # Replication for DR
  enable_replication           = true
  replication_regions          = ["us-west-2", "eu-west-1"]
  replication_repository_filter = "stockxpress/*"
  
  # Notifications
  enable_scan_notifications    = true
  scan_notification_topic_arn  = aws_sns_topic.security_alerts.arn
  
  tags = local.common_tags
}
```

## Architecture

### Repository Structure

```
stockxpress/discovery-server
stockxpress/api-gateway
stockxpress/product-service
stockxpress/inventory-service
stockxpress/order-service
stockxpress/notification-service
```

### Image Tagging Strategy

```
# Development builds
<repository>:dev-<commit-sha>
<repository>:dev-latest

# Staging builds
<repository>:staging-<version>
<repository>:staging-latest

# Production builds
<repository>:prod-<version>
<repository>:production-<version>
<repository>:v1.0.0
<repository>:latest
```

### Lifecycle Policy Rules

1. **Keep last 30 tagged images** (dev, staging, versioned)
2. **Expire untagged images after 7 days**
3. **Keep last 100 production images** (special retention)

## CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Build and Push to ECR

on:
  push:
    branches:
      - main
      - develop

env:
  AWS_REGION: us-east-1
  ECR_REGISTRY: ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.us-east-1.amazonaws.com

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v1
      
      - name: Build, tag, and push image
        env:
          ECR_REPOSITORY: stockxpress/order-service
          IMAGE_TAG: ${{ github.sha }}
        run: |
          # Build Docker image
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          
          # Tag with environment
          if [ "${{ github.ref }}" == "refs/heads/main" ]; then
            docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:prod-$IMAGE_TAG
            docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:latest
            docker push $ECR_REGISTRY/$ECR_REPOSITORY:prod-$IMAGE_TAG
            docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
          else
            docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:dev-$IMAGE_TAG
            docker push $ECR_REGISTRY/$ECR_REPOSITORY:dev-$IMAGE_TAG
          fi
          
          # Push image
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
      
      - name: Scan image for vulnerabilities
        run: |
          # Wait for scan to complete
          aws ecr wait image-scan-complete \
            --repository-name stockxpress/order-service \
            --image-id imageTag=${{ github.sha }}
          
          # Get scan findings
          aws ecr describe-image-scan-findings \
            --repository-name stockxpress/order-service \
            --image-id imageTag=${{ github.sha }}
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    environment {
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = credentials('aws-account-id')
        ECR_REPOSITORY = 'stockxpress/order-service'
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
    }
    
    stages {
        stage('Build') {
            steps {
                script {
                    sh 'docker build -t ${ECR_REPOSITORY}:${IMAGE_TAG} .'
                }
            }
        }
        
        stage('Push to ECR') {
            steps {
                script {
                    sh '''
                        # Login to ECR
                        aws ecr get-login-password --region ${AWS_REGION} | \
                          docker login --username AWS --password-stdin \
                          ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                        
                        # Tag image
                        docker tag ${ECR_REPOSITORY}:${IMAGE_TAG} \
                          ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}:${IMAGE_TAG}
                        
                        # Push image
                        docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}:${IMAGE_TAG}
                    '''
                }
            }
        }
        
        stage('Scan Image') {
            steps {
                script {
                    sh '''
                        # Trigger scan
                        aws ecr start-image-scan \
                          --repository-name ${ECR_REPOSITORY} \
                          --image-id imageTag=${IMAGE_TAG} \
                          --region ${AWS_REGION}
                        
                        # Wait for scan
                        aws ecr wait image-scan-complete \
                          --repository-name ${ECR_REPOSITORY} \
                          --image-id imageTag=${IMAGE_TAG} \
                          --region ${AWS_REGION}
                    '''
                }
            }
        }
    }
}
```

## Docker Commands

### Login to ECR

```bash
# AWS CLI v2
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# AWS CLI v1 (deprecated)
$(aws ecr get-login --no-include-email --region us-east-1)
```

### Build and Push Image

```bash
# Set variables
AWS_ACCOUNT_ID=123456789012
AWS_REGION=us-east-1
REPOSITORY=stockxpress/order-service
IMAGE_TAG=v1.0.0

# Build image
docker build -t $REPOSITORY:$IMAGE_TAG .

# Tag for ECR
docker tag $REPOSITORY:$IMAGE_TAG \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY:$IMAGE_TAG

# Push to ECR
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY:$IMAGE_TAG
```

### Pull Image

```bash
# Pull image from ECR
docker pull $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY:$IMAGE_TAG

# Run container
docker run -d \
  -p 8080:8080 \
  --name order-service \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY:$IMAGE_TAG
```

## Image Scanning

### Manual Scan

```bash
# Start image scan
aws ecr start-image-scan \
  --repository-name stockxpress/order-service \
  --image-id imageTag=v1.0.0 \
  --region us-east-1

# Wait for scan to complete
aws ecr wait image-scan-complete \
  --repository-name stockxpress/order-service \
  --image-id imageTag=v1.0.0 \
  --region us-east-1

# Get scan findings
aws ecr describe-image-scan-findings \
  --repository-name stockxpress/order-service \
  --image-id imageTag=v1.0.0 \
  --region us-east-1
```

### Enhanced Scanning (AWS Inspector)

When enabled, provides:
- **Continuous scanning** of images
- **CVE detection** from multiple databases
- **Package vulnerability scanning**
- **Detailed remediation guidance**
- **Integration with Security Hub**

## Kubernetes Integration

### ImagePullSecrets (if needed)

```yaml
# Create secret for ECR authentication
kubectl create secret docker-registry ecr-credentials \
  --docker-server=123456789012.dkr.ecr.us-east-1.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password --region us-east-1) \
  --namespace=default

# Use in pod spec
apiVersion: v1
kind: Pod
metadata:
  name: order-service
spec:
  containers:
  - name: order-service
    image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/stockxpress/order-service:v1.0.0
  imagePullSecrets:
  - name: ecr-credentials
```

### Using IAM Roles (EKS)

```yaml
# No imagePullSecrets needed with IRSA
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      serviceAccountName: order-service-sa  # SA with ECR permissions
      containers:
      - name: order-service
        image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/stockxpress/order-service:v1.0.0
        ports:
        - containerPort: 8080
```

## IAM Policies

### CI/CD Push Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeImages",
        "ecr:StartImageScan"
      ],
      "Resource": "arn:aws:ecr:us-east-1:123456789012:repository/stockxpress/*"
    }
  ]
}
```

### EKS Pull Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "arn:aws:ecr:us-east-1:123456789012:repository/stockxpress/*"
    }
  ]
}
```

## Best Practices

1. **Image Tagging**:
   - Use semantic versioning for releases
   - Include commit SHA in dev builds
   - Tag by environment (dev, staging, prod)
   - Avoid overusing `latest` tag

2. **Security**:
   - Enable image scanning on push
   - Use enhanced scanning for production
   - Set up notifications for critical vulnerabilities
   - Use immutable tags for production

3. **Lifecycle Management**:
   - Keep production images longer
   - Clean up untagged images regularly
   - Set appropriate retention based on environment

4. **Cost Optimization**:
   - Enable lifecycle policies to reduce storage
   - Use compression in Docker builds
   - Clean up old images regularly
   - Monitor repository size

5. **Multi-Region**:
   - Enable replication for DR
   - Use pull-through cache for frequently used images

## Troubleshooting

### Authentication Issues

```bash
# Verify AWS credentials
aws sts get-caller-identity

# Re-login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com
```

### Push Denied

```bash
# Check repository exists
aws ecr describe-repositories --repository-names stockxpress/order-service

# Verify IAM permissions
aws ecr get-repository-policy --repository-name stockxpress/order-service
```

### Scan Failures

```bash
# Check scan status
aws ecr describe-image-scan-findings \
  --repository-name stockxpress/order-service \
  --image-id imageTag=v1.0.0

# Retry scan
aws ecr start-image-scan \
  --repository-name stockxpress/order-service \
  --image-id imageTag=v1.0.0
```

## Cost Optimization

- **Storage**: $0.10 per GB-month
- **Data Transfer**: Standard AWS data transfer rates
- **Scanning**: Basic scanning included, enhanced scanning separate cost

### Savings Tips:
1. Enable lifecycle policies (can save 40-60% on storage)
2. Use multi-stage Docker builds to reduce image size
3. Clean up old development images
4. Monitor with AWS Cost Explorer

## References

- [AWS ECR Documentation](https://docs.aws.amazon.com/ecr/)
- [ECR Best Practices](https://docs.aws.amazon.com/AmazonECR/latest/userguide/best-practices.html)
- [Docker Documentation](https://docs.docker.com/)
- [Image Scanning](https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-scanning.html)
