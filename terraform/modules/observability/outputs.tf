output "flow_logs_bucket_name" {
  value       = aws_s3_bucket.flow_logs.bucket
  description = "Name of the bucket storing VPC flow logs"
}

output "flow_log_id" {
  value       = aws_flow_log.main.id
  description = "ID of the VPC flow log resource"
}
