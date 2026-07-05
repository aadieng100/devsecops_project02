# Output names are kept identical to the legacy infra-network root so the CI
# pipeline (`terraform output -raw ...`) continues to work unchanged.

output "vpc_id" {
  value       = module.network.vpc_id
  description = "The core primary network identifier"
}

output "public_subnet_ids" {
  value       = module.network.public_subnet_ids
  description = "IDs of the public ingress subnets"
}

output "private_subnet_ids" {
  value       = module.network.private_subnet_ids
  description = "IDs of the private application compute subnets"
}

output "database_subnet_ids" {
  value       = module.network.database_subnet_ids
  description = "IDs of the isolated database subnets"
}

output "alb_dns_name" {
  value       = module.compute.alb_dns_name
  description = "Public entrypoint URL of the e-commerce engine endpoint"
}

output "alb_arn" {
  value       = module.compute.alb_arn
  description = "ARN of the perimeter ALB"
}

output "target_group_arn" {
  value       = module.compute.target_group_arn
  description = "ARN of the application target group"
}

output "app_deploy_bucket_name" {
  value       = module.compute.app_deploy_bucket_name
  description = "Name of the ephemeral deployment storage bucket"
}

output "db_instance_endpoint" {
  value       = module.database.db_instance_endpoint
  description = "Database network connection routing handle"
}

output "db_instance_address" {
  value       = module.database.db_instance_address
  description = "DNS address of the database instance"
}
