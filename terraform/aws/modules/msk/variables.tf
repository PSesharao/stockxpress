# MSK (Kafka) Module Variables

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, production)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where MSK will be deployed"
  type        = string
}

variable "subnet_ids" {
  description = "List of subnet IDs for MSK brokers (should be in different AZs)"
  type        = list(string)
}

variable "kafka_version" {
  description = "Kafka version"
  type        = string
  default     = "3.5.1"
}

variable "broker_instance_type" {
  description = "Instance type for Kafka brokers"
  type        = string
  default     = "kafka.m5.large"
}

variable "number_of_broker_nodes" {
  description = "Number of broker nodes (must be multiple of AZs)"
  type        = number
  default     = 3
}

variable "broker_volume_size" {
  description = "EBS volume size for each broker (GB)"
  type        = number
  default     = 100
}

variable "enable_provisioned_throughput" {
  description = "Enable EBS provisioned throughput"
  type        = bool
  default     = false
}

variable "provisioned_throughput_mibps" {
  description = "Provisioned throughput in MiB/s (only if enabled)"
  type        = number
  default     = 250
}

# Encryption
variable "create_kms_key" {
  description = "Create KMS key for encryption"
  type        = bool
  default     = true
}

variable "kms_key_arn" {
  description = "KMS key ARN for encryption (if not creating new key)"
  type        = string
  default     = null
}

variable "client_broker_encryption" {
  description = "Encryption setting for client-broker communication (TLS, TLS_PLAINTEXT, PLAINTEXT)"
  type        = string
  default     = "TLS"
  
  validation {
    condition     = contains(["TLS", "TLS_PLAINTEXT", "PLAINTEXT"], var.client_broker_encryption)
    error_message = "Must be TLS, TLS_PLAINTEXT, or PLAINTEXT."
  }
}

# Authentication
variable "enable_scram_authentication" {
  description = "Enable SASL/SCRAM authentication"
  type        = bool
  default     = true
}

variable "enable_iam_authentication" {
  description = "Enable IAM authentication"
  type        = bool
  default     = true
}

variable "enable_unauthenticated_access" {
  description = "Enable unauthenticated access"
  type        = bool
  default     = false
}

variable "scram_username" {
  description = "SCRAM username"
  type        = string
  default     = "kafka-admin"
}

variable "scram_secret_arn" {
  description = "ARN of the existing AWS Secrets Manager secret containing SCRAM credentials. MUST be created externally before running Terraform. Secret format: {\"username\":\"kafka-admin\",\"password\":\"secure-password\"}. Never generate passwords in Terraform."
  type        = string
  default     = null
  sensitive   = true
}

# Kafka Configuration
variable "auto_create_topics_enable" {
  description = "Enable auto creation of topics"
  type        = bool
  default     = false
}

variable "default_replication_factor" {
  description = "Default replication factor for topics"
  type        = number
  default     = 3
}

variable "min_insync_replicas" {
  description = "Minimum number of in-sync replicas"
  type        = number
  default     = 2
}

variable "num_partitions" {
  description = "Default number of partitions for new topics"
  type        = number
  default     = 3
}

variable "log_retention_hours" {
  description = "Number of hours to retain Kafka logs"
  type        = number
  default     = 168  # 7 days
}

variable "log_segment_hours" {
  description = "Number of hours before log segment rolls over"
  type        = number
  default     = 24
}

variable "compression_type" {
  description = "Compression type for topics (gzip, snappy, lz4, zstd, producer, uncompressed)"
  type        = string
  default     = "producer"
}

variable "max_message_bytes" {
  description = "Maximum size of message that broker can receive"
  type        = number
  default     = 1048576  # 1 MB
}

# Monitoring
variable "enable_jmx_exporter" {
  description = "Enable JMX exporter for Prometheus"
  type        = bool
  default     = true
}

variable "enable_node_exporter" {
  description = "Enable Node exporter for Prometheus"
  type        = bool
  default     = true
}

variable "enable_cloudwatch_alarms" {
  description = "Enable CloudWatch alarms for MSK"
  type        = bool
  default     = true
}

variable "alarm_actions" {
  description = "List of ARNs for alarm actions (SNS topics)"
  type        = list(string)
  default     = []
}

# Logging
variable "enable_cloudwatch_logs" {
  description = "Enable CloudWatch logs for brokers"
  type        = bool
  default     = true
}

variable "enable_firehose_logs" {
  description = "Enable Kinesis Firehose logs"
  type        = bool
  default     = false
}

variable "enable_s3_logs" {
  description = "Enable S3 logs"
  type        = bool
  default     = false
}

variable "firehose_delivery_stream_name" {
  description = "Kinesis Firehose delivery stream name"
  type        = string
  default     = null
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 7
}

variable "cloudwatch_log_kms_key_id" {
  description = "KMS key ID for CloudWatch logs encryption"
  type        = string
  default     = null
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
