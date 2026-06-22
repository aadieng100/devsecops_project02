output "s3_bucket_name" {
  value       = aws_s3_bucket.state_bucket.id
  description = "The globally unique name of the secure remote state S3 bucket"
}

output "github_actions_role_arn" {
  value       = aws_iam_role.github_actions_role.arn
  description = "The explicit IAM Role ARN for GitHub Actions configuration injection"
}
