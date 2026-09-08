# ECR (Elastic Container Registry) Repositories
# Provides Docker image repositories for microservices

# KMS key for ECR encryption
resource "aws_kms_key" "ecr" {
  count               = var.create_kms_key ? 1 : 0
  description         = "KMS key for ECR repository encryption"
  enable_key_rotation = true

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-ecr-kms"
    }
  )
}

resource "aws_kms_alias" "ecr" {
  count         = var.create_kms_key ? 1 : 0
  name          = "alias/${var.project_name}-${var.environment}-ecr"
  target_key_id = aws_kms_key.ecr[0].key_id
}

# ECR Repositories
resource "aws_ecr_repository" "services" {
  for_each = toset(var.repositories)
  
  name                 = "${var.project_name}/${each.key}"
  image_tag_mutability = var.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = var.scan_on_push
  }

  encryption_configuration {
    encryption_type = var.create_kms_key || var.kms_key_arn != null ? "KMS" : "AES256"
    kms_key         = var.create_kms_key ? aws_kms_key.ecr[0].arn : var.kms_key_arn
  }

  tags = merge(
    var.tags,
    {
      Name    = "${var.project_name}/${each.key}"
      Service = each.key
    }
  )
}

# Lifecycle Policy for ECR repositories
resource "aws_ecr_lifecycle_policy" "services" {
  for_each   = toset(var.repositories)
  repository = aws_ecr_repository.services[each.key].name

  policy = jsonencode({
    rules = concat(
      # Keep last N tagged images
      [{
        rulePriority = 1
        description  = "Keep last ${var.max_image_count} tagged images"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = var.tag_prefix_list
          countType     = "imageCountMoreThan"
          countNumber   = var.max_image_count
        }
        action = {
          type = "expire"
        }
      }],
      # Expire untagged images
      [{
        rulePriority = 2
        description  = "Expire untagged images older than ${var.untagged_image_days} days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = var.untagged_image_days
        }
        action = {
          type = "expire"
        }
      }],
      # Keep production images longer
      var.keep_production_images ? [{
        rulePriority = 3
        description  = "Keep production images"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["prod-", "production-"]
          countType     = "imageCountMoreThan"
          countNumber   = var.max_production_image_count
        }
        action = {
          type = "expire"
        }
      }] : []
    )
  })
}

# Repository Policy for cross-account access
resource "aws_ecr_repository_policy" "services" {
  for_each   = var.enable_cross_account_access ? toset(var.repositories) : []
  repository = aws_ecr_repository.services[each.key].name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCrossAccountPull"
        Effect = "Allow"
        Principal = {
          AWS = var.cross_account_principals
        }
        Action = [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability",
          "ecr:DescribeRepositories",
          "ecr:GetRepositoryPolicy",
          "ecr:ListImages"
        ]
      }
    ]
  })
}

# Replication configuration (if enabled)
resource "aws_ecr_replication_configuration" "main" {
  count = var.enable_replication ? 1 : 0

  replication_configuration {
    rule {
      dynamic "destination" {
        for_each = var.replication_regions
        content {
          region      = destination.value
          registry_id = data.aws_caller_identity.current.account_id
        }
      }

      repository_filter {
        filter      = var.replication_repository_filter
        filter_type = "PREFIX_MATCH"
      }
    }
  }
}

# Registry Scanning Configuration
resource "aws_ecr_registry_scanning_configuration" "main" {
  count = var.enable_enhanced_scanning ? 1 : 0

  scan_type = "ENHANCED"

  rule {
    scan_frequency = var.enhanced_scan_frequency
    
    repository_filter {
      filter      = "${var.project_name}/*"
      filter_type = "WILDCARD"
    }
  }
}

# Registry Policy for permissions
resource "aws_ecr_registry_policy" "main" {
  count = var.enable_registry_policy ? 1 : 0

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowPullFromOrganization"
        Effect = "Allow"
        Principal = {
          AWS = "*"
        }
        Action = [
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer"
        ]
        Condition = {
          StringEquals = {
            "aws:PrincipalOrgID" = var.organization_id
          }
        }
      }
    ]
  })
}

# CloudWatch Log Group for ECR API logs (if enabled)
resource "aws_cloudwatch_log_group" "ecr_api" {
  count             = var.enable_api_logging ? 1 : 0
  name              = "/aws/ecr/${var.project_name}-${var.environment}"
  retention_in_days = var.log_retention_days
  kms_key_id        = var.cloudwatch_log_kms_key_id

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-ecr-api-logs"
    }
  )
}

# EventBridge rule for image scan findings (optional)
resource "aws_cloudwatch_event_rule" "ecr_scan_findings" {
  count       = var.enable_scan_notifications ? 1 : 0
  name        = "${var.project_name}-${var.environment}-ecr-scan-findings"
  description = "Capture ECR image scan findings"

  event_pattern = jsonencode({
    source      = ["aws.ecr"]
    detail-type = ["ECR Image Scan"]
    detail = {
      repository-name = [for repo in var.repositories : "${var.project_name}/${repo}"]
      scan-status     = ["COMPLETE"]
      finding-severity-counts = {
        CRITICAL = [{ "numeric" : [">", 0] }]
      }
    }
  })

  tags = var.tags
}

resource "aws_cloudwatch_event_target" "ecr_scan_findings" {
  count     = var.enable_scan_notifications ? 1 : 0
  rule      = aws_cloudwatch_event_rule.ecr_scan_findings[0].name
  target_id = "SendToSNS"
  arn       = var.scan_notification_topic_arn
}

# Data sources
data "aws_caller_identity" "current" {}
