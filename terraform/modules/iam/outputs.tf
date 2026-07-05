output "instance_profile_arn" {
  value       = aws_iam_instance_profile.ec2_profile.arn
  description = "ARN of the EC2 instance profile to attach to the launch template"
}

output "instance_profile_name" {
  value       = aws_iam_instance_profile.ec2_profile.name
  description = "Name of the EC2 instance profile"
}

output "role_name" {
  value       = aws_iam_role.ec2_s3_role.name
  description = "Name of the EC2 IAM role"
}
