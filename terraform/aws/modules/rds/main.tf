# RDS Module - Main Configuration
# Creates MySQL RDS instances with automated backups, encryption, and Multi-AZ support

data "aws_partition" "current" {}

# SECURITY: Passwords MUST be managed externally via AWS Secrets Manager
# DO NOT generate passwords in Terraform code
# Create secrets before running Terraform:
#   aws secretsmanager create-secret --name "stockxpress/ENV/rds-DB_NAME-password" \
#     --secret-string "$(openssl rand -base64 32)"
#
# Then retrieve in Terraform using data source:
#   data "aws_secretsmanager_secret_version" "db_password" {
#     secret_id = "stockxpress/${var.environment}/rds-${each.key}-password"
#   }

# KMS Key for RDS encryption
resource "aws_kms_key" "rds" {
  for_each = var.databases

  description             = "RDS encryption key for ${each.key} database"
  deletion_window_in_days = var.kms_key_deletion_window
  enable_key_rotation     = true

  tags = merge(
    var.tags,
    {
      Name     = "${var.name_prefix}-${each.key}-rds-key"
      Database = each.key
    }
  )
}

resource "aws_kms_alias" "rds" {
  for_each = var.databases

  name          = "alias/${var.name_prefix}-${each.key}-rds"
  target_key_id = aws_kms_key.rds[each.key].key_id
}

# RDS Security Group
resource "aws_security_group" "rds" {
  for_each = var.databases

  name_prefix = "${var.name_prefix}-${each.key}-rds-sg-"
  description = "Security group for ${each.key} RDS instance"
  vpc_id      = var.vpc_id

  tags = merge(
    var.tags,
    {
      Name     = "${var.name_prefix}-${each.key}-rds-sg"
      Database = each.key
    }
  )

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "rds_ingress_mysql" {
  for_each = var.databases

  type              = "ingress"
  from_port         = 3306
  to_port           = 3306
  protocol          = "tcp"
  cidr_blocks       = lookup(each.value, "allowed_cidr_blocks", var.default_allowed_cidr_blocks)
  security_group_id = aws_security_group.rds[each.key].id
  description       = "Allow MySQL traffic from specified CIDR blocks"
}

resource "aws_security_group_rule" "rds_ingress_security_groups" {
  for_each = {
    for pair in flatten([
      for db_key, db_config in var.databases : [
        for sg in lookup(db_config, "allowed_security_groups", []) : {
          db_key = db_key
          sg_id  = sg
          key    = "${db_key}-${sg}"
        }
      ]
    ]) : pair.key => pair
  }

  type                     = "ingress"
  from_port                = 3306
  to_port                  = 3306
  protocol                 = "tcp"
  source_security_group_id = each.value.sg_id
  security_group_id        = aws_security_group.rds[each.value.db_key].id
  description              = "Allow MySQL traffic from security group ${each.value.sg_id}"
}

resource "aws_security_group_rule" "rds_egress" {
  for_each = var.databases

  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.rds[each.key].id
  description       = "Allow all outbound traffic"
}

# RDS Parameter Group
resource "aws_db_parameter_group" "main" {
  for_each = var.databases

  name_prefix = "${var.name_prefix}-${each.key}-"
  family      = lookup(each.value, "parameter_group_family", var.default_parameter_group_family)
  description = "Custom parameter group for ${each.key} database"

  dynamic "parameter" {
    for_each = lookup(each.value, "parameters", var.default_parameters)
    content {
      name  = parameter.value.name
      value = parameter.value.value
      apply_method = lookup(parameter.value, "apply_method", "immediate")
    }
  }

  tags = merge(
    var.tags,
    {
      Name     = "${var.name_prefix}-${each.key}-pg"
      Database = each.key
    }
  )

  lifecycle {
    create_before_destroy = true
  }
}

# RDS Option Group
resource "aws_db_option_group" "main" {
  for_each = var.databases

  name_prefix              = "${var.name_prefix}-${each.key}-"
  option_group_description = "Option group for ${each.key} database"
  engine_name              = lookup(each.value, "engine", var.default_engine)
  major_engine_version     = lookup(each.value, "major_engine_version", var.default_major_engine_version)

  dynamic "option" {
    for_each = lookup(each.value, "options", [])
    content {
      option_name = option.value.option_name
      
      dynamic "option_settings" {
        for_each = lookup(option.value, "option_settings", [])
        content {
          name  = option_settings.value.name
          value = option_settings.value.value
        }
      }
    }
  }

  tags = merge(
    var.tags,
    {
      Name     = "${var.name_prefix}-${each.key}-og"
      Database = each.key
    }
  )

  lifecycle {
    create_before_destroy = true
  }
}

# RDS Instance
resource "aws_db_instance" "main" {
  for_each = var.databases

  identifier     = "${var.name_prefix}-${each.key}"
  engine         = lookup(each.value, "engine", var.default_engine)
  engine_version = lookup(each.value, "engine_version", var.default_engine_version)
  instance_class = lookup(each.value, "instance_class", var.default_instance_class)

  db_name  = lookup(each.value, "database_name", each.key)
  username = lookup(each.value, "master_username", var.default_master_username)
  # SECURITY: Password MUST be provided via variable from AWS Secrets Manager
  # Never use random_password or hardcoded values
  # Use: password = lookup(each.value, "master_password", null)
  # And provide via: databases = { order = { master_password = data.aws_secretsmanager_secret_version.order.secret_string } }
  password = lookup(each.value, "master_password", null)
  port     = lookup(each.value, "port", 3306)

  allocated_storage     = lookup(each.value, "allocated_storage", var.default_allocated_storage)
  max_allocated_storage = lookup(each.value, "max_allocated_storage", var.default_max_allocated_storage)
  storage_type          = lookup(each.value, "storage_type", var.default_storage_type)
  storage_encrypted     = lookup(each.value, "storage_encrypted", true)
  kms_key_id            = aws_kms_key.rds[each.key].arn
  iops                  = lookup(each.value, "storage_type", var.default_storage_type) == "io1" ? lookup(each.value, "iops", 3000) : null
  storage_throughput    = lookup(each.value, "storage_type", var.default_storage_type) == "gp3" ? lookup(each.value, "storage_throughput", 125) : null

  multi_az               = lookup(each.value, "multi_az", var.default_multi_az)
  db_subnet_group_name   = var.db_subnet_group_name
  vpc_security_group_ids = [aws_security_group.rds[each.key].id]
  publicly_accessible    = lookup(each.value, "publicly_accessible", false)

  parameter_group_name = aws_db_parameter_group.main[each.key].name
  option_group_name    = aws_db_option_group.main[each.key].name

  # Backup configuration
  backup_retention_period   = lookup(each.value, "backup_retention_period", var.default_backup_retention_period)
  backup_window             = lookup(each.value, "backup_window", var.default_backup_window)
  maintenance_window        = lookup(each.value, "maintenance_window", var.default_maintenance_window)
  copy_tags_to_snapshot     = true
  skip_final_snapshot       = lookup(each.value, "skip_final_snapshot", var.default_skip_final_snapshot)
  final_snapshot_identifier = lookup(each.value, "skip_final_snapshot", var.default_skip_final_snapshot) ? null : "${var.name_prefix}-${each.key}-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"

  # Enhanced monitoring
  enabled_cloudwatch_logs_exports = lookup(each.value, "enabled_cloudwatch_logs_exports", var.default_enabled_cloudwatch_logs_exports)
  monitoring_interval             = lookup(each.value, "monitoring_interval", var.default_monitoring_interval)
  monitoring_role_arn             = var.default_monitoring_interval > 0 || lookup(each.value, "monitoring_interval", 0) > 0 ? aws_iam_role.rds_monitoring[0].arn : null

  # Performance Insights
  performance_insights_enabled    = lookup(each.value, "performance_insights_enabled", var.default_performance_insights_enabled)
  performance_insights_kms_key_id = lookup(each.value, "performance_insights_enabled", var.default_performance_insights_enabled) ? aws_kms_key.rds[each.key].arn : null
  performance_insights_retention_period = lookup(each.value, "performance_insights_retention_period", var.default_performance_insights_retention_period)

  # Additional settings
  auto_minor_version_upgrade = lookup(each.value, "auto_minor_version_upgrade", var.default_auto_minor_version_upgrade)
  deletion_protection        = lookup(each.value, "deletion_protection", var.default_deletion_protection)
  apply_immediately          = lookup(each.value, "apply_immediately", false)

  tags = merge(
    var.tags,
    {
      Name     = "${var.name_prefix}-${each.key}"
      Database = each.key
    }
  )

  lifecycle {
    ignore_changes = [final_snapshot_identifier]
  }
}

# CloudWatch Alarms for RDS monitoring
resource "aws_cloudwatch_metric_alarm" "cpu" {
  for_each = var.enable_cloudwatch_alarms ? var.databases : {}

  alarm_name          = "${var.name_prefix}-${each.key}-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = lookup(each.value, "cpu_alarm_threshold", var.default_cpu_alarm_threshold)
  alarm_description   = "This metric monitors ${each.key} database CPU utilization"
  alarm_actions       = var.alarm_actions

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main[each.key].id
  }

  tags = merge(
    var.tags,
    {
      Database = each.key
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "storage" {
  for_each = var.enable_cloudwatch_alarms ? var.databases : {}

  alarm_name          = "${var.name_prefix}-${each.key}-low-storage"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = lookup(each.value, "storage_alarm_threshold", var.default_storage_alarm_threshold)
  alarm_description   = "This metric monitors ${each.key} database free storage space"
  alarm_actions       = var.alarm_actions

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main[each.key].id
  }

  tags = merge(
    var.tags,
    {
      Database = each.key
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "memory" {
  for_each = var.enable_cloudwatch_alarms ? var.databases : {}

  alarm_name          = "${var.name_prefix}-${each.key}-low-memory"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "FreeableMemory"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = lookup(each.value, "memory_alarm_threshold", var.default_memory_alarm_threshold)
  alarm_description   = "This metric monitors ${each.key} database freeable memory"
  alarm_actions       = var.alarm_actions

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main[each.key].id
  }

  tags = merge(
    var.tags,
    {
      Database = each.key
    }
  )
}

# IAM Role for Enhanced Monitoring
resource "aws_iam_role" "rds_monitoring" {
  count = var.default_monitoring_interval > 0 ? 1 : 0
  name  = "${var.name_prefix}-rds-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  count      = var.default_monitoring_interval > 0 ? 1 : 0
  role       = aws_iam_role.rds_monitoring[0].name
  policy_arn = "arn:${data.aws_partition.current.partition}:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# Secrets Manager for storing database credentials
resource "aws_secretsmanager_secret" "db_password" {
  for_each = var.store_master_password_in_secrets_manager ? var.databases : {}

  name_prefix             = "${var.name_prefix}-${each.key}-password-"
  description             = "Master password for ${each.key} database"
  recovery_window_in_days = var.secret_recovery_window_days
  kms_key_id              = aws_kms_key.rds[each.key].id

  tags = merge(
    var.tags,
    {
      Name     = "${var.name_prefix}-${each.key}-password"
      Database = each.key
    }
  )
}

resource "aws_secretsmanager_secret_version" "db_password" {
  for_each = var.store_master_password_in_secrets_manager ? var.databases : {}

  secret_id = aws_secretsmanager_secret.db_password[each.key].id
  secret_string = jsonencode({
    username            = aws_db_instance.main[each.key].username
    password            = random_password.master[each.key].result
    engine              = aws_db_instance.main[each.key].engine
    host                = aws_db_instance.main[each.key].address
    port                = aws_db_instance.main[each.key].port
    dbname              = aws_db_instance.main[each.key].db_name
    dbInstanceIdentifier = aws_db_instance.main[each.key].id
  })
}
