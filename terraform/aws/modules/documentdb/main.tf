# DocumentDB Cluster for Product Service (MongoDB-compatible)
# Provides managed MongoDB-compatible database for product catalog

# SECURITY: Passwords MUST be managed externally via AWS Secrets Manager
# DO NOT generate passwords in Terraform code
# Create secrets before running Terraform:
#   aws secretsmanager create-secret --name "stockxpress/ENV/documentdb-password" \
#     --secret-string "$(openssl rand -base64 32)"
#
# Then retrieve in Terraform using data source:
#   data "aws_secretsmanager_secret_version" "documentdb_password" {
#     secret_id = "stockxpress/${var.environment}/documentdb-password"
#   }

# Security Group for DocumentDB
resource "aws_security_group" "documentdb" {
  name_prefix = "${var.project_name}-${var.environment}-documentdb-"
  description = "Security group for DocumentDB cluster"
  vpc_id      = var.vpc_id

  ingress {
    description = "MongoDB from VPC"
    from_port   = 27017
    to_port     = 27017
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
      Name = "${var.project_name}-${var.environment}-documentdb-sg"
    }
  )

  lifecycle {
    create_before_destroy = true
  }
}

# Subnet Group for DocumentDB
resource "aws_docdb_subnet_group" "documentdb" {
  name        = "${var.project_name}-${var.environment}-documentdb-subnet-group"
  description = "Subnet group for DocumentDB cluster"
  subnet_ids  = var.database_subnet_ids

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-documentdb-subnet-group"
    }
  )
}

# Cluster Parameter Group for DocumentDB
resource "aws_docdb_cluster_parameter_group" "documentdb" {
  family      = var.cluster_family
  name        = "${var.project_name}-${var.environment}-documentdb-params"
  description = "DocumentDB cluster parameter group for ${var.project_name}"

  # Enable TLS
  parameter {
    name  = "tls"
    value = "enabled"
  }

  # Enable audit logs
  parameter {
    name  = "audit_logs"
    value = "enabled"
  }

  # TTL monitor (for automatic document deletion)
  parameter {
    name  = "ttl_monitor"
    value = "enabled"
  }

  # Profiler settings
  parameter {
    name  = "profiler"
    value = var.enable_profiler ? "enabled" : "disabled"
  }

  parameter {
    name  = "profiler_threshold_ms"
    value = var.profiler_threshold_ms
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-documentdb-params"
    }
  )
}

# DocumentDB Cluster
resource "aws_docdb_cluster" "documentdb" {
  cluster_identifier              = "${var.project_name}-${var.environment}-documentdb"
  engine                          = "docdb"
  engine_version                  = var.engine_version
  master_username                 = var.master_username
  # SECURITY: Password MUST be provided via variable from AWS Secrets Manager
  # Never use random_password or hardcoded values
  # Provide via: master_password = data.aws_secretsmanager_secret_version.documentdb.secret_string
  master_password                 = var.master_password
  backup_retention_period         = var.backup_retention_period
  preferred_backup_window         = var.preferred_backup_window
  preferred_maintenance_window    = var.preferred_maintenance_window
  skip_final_snapshot             = var.skip_final_snapshot
  final_snapshot_identifier       = var.skip_final_snapshot ? null : "${var.project_name}-${var.environment}-documentdb-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"
  db_subnet_group_name            = aws_docdb_subnet_group.documentdb.name
  db_cluster_parameter_group_name = aws_docdb_cluster_parameter_group.documentdb.name
  vpc_security_group_ids          = [aws_security_group.documentdb.id]
  storage_encrypted               = true
  kms_key_id                      = var.kms_key_id
  enabled_cloudwatch_logs_exports = var.enabled_cloudwatch_logs_exports
  deletion_protection             = var.deletion_protection
  apply_immediately               = var.apply_immediately

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-documentdb-cluster"
    }
  )
}

# DocumentDB Cluster Instances
resource "aws_docdb_cluster_instance" "documentdb" {
  count              = var.cluster_size
  identifier         = "${var.project_name}-${var.environment}-documentdb-${count.index + 1}"
  cluster_identifier = aws_docdb_cluster.documentdb.id
  instance_class     = var.instance_class
  promotion_tier     = count.index
  
  auto_minor_version_upgrade = var.auto_minor_version_upgrade
  preferred_maintenance_window = var.preferred_maintenance_window

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-documentdb-instance-${count.index + 1}"
    }
  )
}

# CloudWatch Alarms for DocumentDB
resource "aws_cloudwatch_metric_alarm" "documentdb_cpu" {
  count               = var.enable_cloudwatch_alarms ? var.cluster_size : 0
  alarm_name          = "${var.project_name}-${var.environment}-documentdb-${count.index + 1}-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/DocDB"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors DocumentDB instance CPU utilization"
  alarm_actions       = var.alarm_actions

  dimensions = {
    DBInstanceIdentifier = aws_docdb_cluster_instance.documentdb[count.index].identifier
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "documentdb_connections" {
  count               = var.enable_cloudwatch_alarms ? 1 : 0
  alarm_name          = "${var.project_name}-${var.environment}-documentdb-connections"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "DatabaseConnections"
  namespace           = "AWS/DocDB"
  period              = "300"
  statistic           = "Average"
  threshold           = var.max_connections_threshold
  alarm_description   = "This metric monitors DocumentDB cluster database connections"
  alarm_actions       = var.alarm_actions

  dimensions = {
    DBClusterIdentifier = aws_docdb_cluster.documentdb.cluster_identifier
  }

  tags = var.tags
}

# Data source for VPC
data "aws_vpc" "selected" {
  id = var.vpc_id
}
