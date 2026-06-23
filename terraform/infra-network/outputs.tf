output "vpc_id" {
  value       = aws_vpc.main.id
  description = "The core primary network identifier"
}

output "public_subnet_ids" {
  value       = aws_subnet.public[*].id
  description = "The list of IDs mapping to public ingress subnets"
}

output "private_subnet_ids" {
  value       = aws_subnet.private[*].id
  description = "The list of IDs mapping to secure private application compute subnets"
}

output "database_subnet_ids" {
  value       = aws_subnet.database[*].id
  description = "The list of IDs mapping to completely isolated database subnets"
}

output "alb_dns_name" {
  value       = aws_lb.external.dns_name
  description = "The public entrypoint URL of our headless e-commerce engine endpoint"
}

output "db_instance_endpoint" {
  value       = aws_db_instance.postgres.endpoint
  description = "The complete database network connection routing handle"
}

output "db_instance_address" {
  value       = aws_db_instance.postgres.address
  description = "The specific software-defined DNS address of the database cluster"
}
output "app_deploy_bucket_name" {
  value       = aws_s3_bucket.app_deploy.bucket
  description = "The dynamic name of the ephemeral deployment storage cell"
}

output "alb_arn" {
  value       = aws_lb.external.arn
  description = "The corporate ARN registry handle for the perimeter ALB"
}

output "target_group_arn" {
  value       = aws_lb_target_group.app_tg.arn
  description = "The routing token identifier for the computing application target group"
}
