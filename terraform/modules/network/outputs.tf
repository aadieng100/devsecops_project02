output "vpc_id" {
  value       = aws_vpc.main.id
  description = "The core primary network identifier"
}

output "public_subnet_ids" {
  value       = aws_subnet.public[*].id
  description = "IDs mapping to public ingress subnets"
}

output "private_subnet_ids" {
  value       = aws_subnet.private[*].id
  description = "IDs mapping to secure private application compute subnets"
}

output "database_subnet_ids" {
  value       = aws_subnet.database[*].id
  description = "IDs mapping to completely isolated database subnets"
}

output "alb_security_group_id" {
  value       = aws_security_group.alb.id
  description = "Security group ID guarding the public-facing ALB"
}

output "app_security_group_id" {
  value       = aws_security_group.app.id
  description = "Security group ID guarding the private application tier"
}
