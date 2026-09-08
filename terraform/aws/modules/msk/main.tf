# MSK (Managed Streaming for Kafka) Cluster
# Provides managed Kafka for event-driven architecture

# SECURITY: SCRAM passwords MUST be managed externally via AWS Secrets Manager
# DO NOT generate passwords in Terraform code
# Create SCRAM secret before running Terraform:
#   aws secretsmanager create-secret --name "stockxpress/ENV/msk-scram-password" \
#     --secret-string '{"username":"kafka-admin","password":"'$(openssl rand -base64 32)'"}'
#
# Then retrieve in Terraform using data source:
#   data "aws_secretsmanager_secret_version" "msk_scram" {
#     secret_id = "stockxpress/${var.environment}/msk-scram-password"
#   }

# KMS key for encryption
resource "aws_kms_key" "msk" {
  count               = var.create_kms_key ? 1 : 0
  description         = "KMS key for MSK cluster encryption"
  enable_key_rotation = true

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-msk-kms"
    }
  )
}

resource "aws_kms_alias" "msk" {
  count         = var.create_kms_key ? 1 : 0
  name          = "alias/${var.project_name}-${var.environment}-msk"
  target_key_id = aws_kms_key.msk[0].key_id
}

# Security Group for MSK
resource "aws_security_group" "msk" {
  name_prefix = "${var.project_name}-${var.environment}-msk-"
  description = "Security group for MSK cluster"
  vpc_id      = var.vpc_id

  ingress {
    description = "Kafka plaintext from VPC"
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  ingress {
    description = "Kafka TLS from VPC"
    from_port   = 9094
    to_port     = 9094
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  ingress {
    description = "Kafka SASL/SCRAM from VPC"
    from_port   = 9096
    to_port     = 9096
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  ingress {
    description = "Kafka IAM from VPC"
    from_port   = 9098
    to_port     = 9098
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  ingress {
    description = "Zookeeper from VPC"
    from_port   = 2181
    to_port     = 2181
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  ingress {
    description = "JMX Exporter from VPC"
    from_port   = 11001
    to_port     = 11001
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  ingress {
    description = "Node Exporter from VPC"
    from_port   = 11002
    to_port     = 11002
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-msk-sg"
    }
  )

  lifecycle {
    create_before_destroy = true
  }
}

# CloudWatch Log Group for MSK
resource "aws_cloudwatch_log_group" "msk" {
  name              = "/aws/msk/${var.project_name}-${var.environment}"
  retention_in_days = var.log_retention_days
  kms_key_id        = var.cloudwatch_log_kms_key_id

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-msk-logs"
    }
  )
}

# S3 Bucket for MSK broker logs (optional)
resource "aws_s3_bucket" "msk_logs" {
  count  = var.enable_s3_logs ? 1 : 0
  bucket = "${var.project_name}-${var.environment}-msk-logs-${data.aws_caller_identity.current.account_id}"

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-msk-logs"
    }
  )
}

resource "aws_s3_bucket_versioning" "msk_logs" {
  count  = var.enable_s3_logs ? 1 : 0
  bucket = aws_s3_bucket.msk_logs[0].id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "msk_logs" {
  count  = var.enable_s3_logs ? 1 : 0
  bucket = aws_s3_bucket.msk_logs[0].id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "msk_logs" {
  count  = var.enable_s3_logs ? 1 : 0
  bucket = aws_s3_bucket.msk_logs[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# MSK Configuration
resource "aws_msk_configuration" "kafka" {
  name              = "${var.project_name}-${var.environment}-msk-config"
  kafka_versions    = [var.kafka_version]
  server_properties = templatefile("${path.module}/templates/server.properties.tpl", {
    auto_create_topics_enable  = var.auto_create_topics_enable
    default_replication_factor = var.default_replication_factor
    min_insync_replicas        = var.min_insync_replicas
    num_partitions             = var.num_partitions
    log_retention_hours        = var.log_retention_hours
    log_segment_hours          = var.log_segment_hours
    compression_type           = var.compression_type
    max_message_bytes          = var.max_message_bytes
  })

  description = "MSK configuration for ${var.project_name} ${var.environment}"
}

# MSK Cluster
resource "aws_msk_cluster" "kafka" {
  cluster_name           = "${var.project_name}-${var.environment}-msk"
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.number_of_broker_nodes

  broker_node_group_info {
    instance_type   = var.broker_instance_type
    client_subnets  = var.subnet_ids
    security_groups = [aws_security_group.msk.id]

    storage_info {
      ebs_storage_info {
        volume_size            = var.broker_volume_size
        provisioned_throughput {
          enabled           = var.enable_provisioned_throughput
          volume_throughput = var.enable_provisioned_throughput ? var.provisioned_throughput_mibps : null
        }
      }
    }

    connectivity_info {
      public_access {
        type = "DISABLED"
      }
    }
  }

  configuration_info {
    arn      = aws_msk_configuration.kafka.arn
    revision = aws_msk_configuration.kafka.latest_revision
  }

  encryption_info {
    encryption_in_transit {
      client_broker = var.client_broker_encryption
      in_cluster    = true
    }

    encryption_at_rest_kms_key_arn = var.create_kms_key ? aws_kms_key.msk[0].arn : var.kms_key_arn
  }

  client_authentication {
    sasl {
      scram = var.enable_scram_authentication
      iam   = var.enable_iam_authentication
    }
    
    unauthenticated = var.enable_unauthenticated_access
  }

  open_monitoring {
    prometheus {
      jmx_exporter {
        enabled_in_broker = var.enable_jmx_exporter
      }
      node_exporter {
        enabled_in_broker = var.enable_node_exporter
      }
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = var.enable_cloudwatch_logs
        log_group = aws_cloudwatch_log_group.msk.name
      }

      firehose {
        enabled         = var.enable_firehose_logs
        delivery_stream = var.firehose_delivery_stream_name
      }

      s3 {
        enabled = var.enable_s3_logs
        bucket  = var.enable_s3_logs ? aws_s3_bucket.msk_logs[0].id : null
        prefix  = var.enable_s3_logs ? "msk-logs/" : null
      }
    }
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-msk"
    }
  )
}

# SECURITY: SCRAM secrets MUST be created manually BEFORE running Terraform
# Terraform should ONLY reference existing secrets, not create them
#
# Create SCRAM secret before running Terraform:
#   aws secretsmanager create-secret \
#     --name "AmazonMSK_stockxpress_ENV" \
#     --description "SCRAM credentials for MSK cluster" \
#     --secret-string '{"username":"kafka-admin","password":"'$(openssl rand -base64 32)'"}' \
#     --region us-east-1
#
# Then attach policy allowing MSK to read it:
#   aws secretsmanager put-resource-policy \
#     --secret-id "AmazonMSK_stockxpress_ENV" \
#     --resource-policy '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"kafka.amazonaws.com"},"Action":"secretsmanager:GetSecretValue","Resource":"*"}]}'
#
# Reference the existing secret ARN via variable: var.scram_secret_arn

# Data source to verify secret exists (optional)
data "aws_secretsmanager_secret" "msk_scram" {
  count = var.enable_scram_authentication ? 1 : 0
  arn   = var.scram_secret_arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AWSKafkaResourcePolicy"
        Effect = "Allow"
        Principal = {
          Service = "kafka.amazonaws.com"
        }
        Action   = "secretsmanager:GetSecretValue"
        Resource = "*"
      }
    ]
  })
}

resource "aws_msk_scram_secret_association" "kafka" {
  count              = var.enable_scram_authentication ? 1 : 0
  cluster_arn        = aws_msk_cluster.kafka.arn
  secret_arn_list    = [aws_secretsmanager_secret.msk_scram[0].arn]

  depends_on = [
    aws_secretsmanager_secret_version.msk_scram
  ]
}

# CloudWatch Alarms for MSK
resource "aws_cloudwatch_metric_alarm" "kafka_cpu" {
  count               = var.enable_cloudwatch_alarms ? 1 : 0
  alarm_name          = "${var.project_name}-${var.environment}-msk-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CpuUser"
  namespace           = "AWS/Kafka"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors MSK broker CPU utilization"
  alarm_actions       = var.alarm_actions

  dimensions = {
    "Cluster Name" = aws_msk_cluster.kafka.cluster_name
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "kafka_disk" {
  count               = var.enable_cloudwatch_alarms ? 1 : 0
  alarm_name          = "${var.project_name}-${var.environment}-msk-disk"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "KafkaDataLogsDiskUsed"
  namespace           = "AWS/Kafka"
  period              = "300"
  statistic           = "Average"
  threshold           = "85"
  alarm_description   = "This metric monitors MSK disk usage percentage"
  alarm_actions       = var.alarm_actions

  dimensions = {
    "Cluster Name" = aws_msk_cluster.kafka.cluster_name
  }

  tags = var.tags
}

# Data sources
data "aws_vpc" "selected" {
  id = var.vpc_id
}

data "aws_caller_identity" "current" {}
