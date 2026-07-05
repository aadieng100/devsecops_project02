output "alb_dns_name" {
  value       = aws_lb.external.dns_name
  description = "Public entrypoint URL of the headless e-commerce engine endpoint"
}

output "alb_arn" {
  value       = aws_lb.external.arn
  description = "ARN of the perimeter ALB"
}

output "target_group_arn" {
  value       = aws_lb_target_group.app_tg.arn
  description = "ARN of the application target group"
}

output "app_deploy_bucket_name" {
  value       = aws_s3_bucket.app_deploy.bucket
  description = "Name of the ephemeral deployment storage bucket"
}

output "autoscaling_group_name" {
  value       = aws_autoscaling_group.app_asg.name
  description = "Name of the application Auto Scaling Group"
}
