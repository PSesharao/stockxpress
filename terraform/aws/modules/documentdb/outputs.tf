# DocumentDB Module Outputs

output "cluster_id" {
  description = "DocumentDB cluster identifier"
  value       = aws_docdb_cluster.documentdb.id
}

output "cluster_arn" {
  description = "DocumentDB cluster ARN"
  value       = aws_docdb_cluster.documentdb.arn
}

output "cluster_endpoint" {
  description = "DocumentDB cluster endpoint"
  value       = aws_docdb_cluster.documentdb.endpoint
}

output "cluster_reader_endpoint" {
  description = "DocumentDB cluster reader endpoint"
  value       = aws_docdb_cluster.documentdb.reader_endpoint
}

output "cluster_port" {
  description = "DocumentDB cluster port"
  value       = aws_docdb_cluster.documentdb.port
}

output "master_username" {
  description = "DocumentDB master username"
  value       = aws_docdb_cluster.documentdb.master_username
  sensitive   = true
}

output "master_password" {
  description = "DocumentDB master password"
  value       = random_password.documentdb_password.result
  sensitive   = true
}

output "security_group_id" {
  description = "Security group ID for DocumentDB"
  value       = aws_security_group.documentdb.id
}

output "subnet_group_name" {
  description = "DocumentDB subnet group name"
  value       = aws_docdb_subnet_group.documentdb.name
}

output "cluster_resource_id" {
  description = "DocumentDB cluster resource ID"
  value       = aws_docdb_cluster.documentdb.cluster_resource_id
}

output "instance_identifiers" {
  description = "List of DocumentDB instance identifiers"
  value       = aws_docdb_cluster_instance.documentdb[*].identifier
}

output "instance_endpoints" {
  description = "List of DocumentDB instance endpoints"
  value       = aws_docdb_cluster_instance.documentdb[*].endpoint
}

output "connection_string" {
  description = "DocumentDB connection string (without password)"
  value       = "mongodb://${aws_docdb_cluster.documentdb.master_username}:<password>@${aws_docdb_cluster.documentdb.endpoint}:${aws_docdb_cluster.documentdb.port}/?tls=true&tlsCAFile=global-bundle.pem&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false"
  sensitive   = true
}
