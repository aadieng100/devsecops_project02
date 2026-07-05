output "db_instance_endpoint" {
  value       = aws_db_instance.postgres.endpoint
  description = "The complete database network connection routing handle (host:port)"
}

output "db_instance_address" {
  value       = aws_db_instance.postgres.address
  description = "The DNS address of the database instance"
}

output "db_security_group_id" {
  value       = aws_security_group.db.id
  description = "Security group ID protecting the database"
}

output "db_password" {
  value       = random_password.db_password.result
  description = "The generated master password (consumed by the compute tier bootstrap)"
  sensitive   = true
}

output "ssm_password_parameter_name" {
  value       = aws_ssm_parameter.db_password.name
  description = "SSM path where the DB password is stored"
}
