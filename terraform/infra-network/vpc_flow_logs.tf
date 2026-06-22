# 1. Dedicated, Cost-Optimized S3 Bucket for Traffic Logs
resource "aws_s3_bucket" "flow_logs" {
  bucket        = "${var.project_name}-vpc-flow-logs-99"
  force_destroy = true # Allows easy clean teardown at project closure

  tags = { Name = "${var.project_name}-flow-logs-bucket" }
}

# 2. Strict Public Access Block for Log Integrity
resource "aws_s3_bucket_public_access_block" "flow_logs_public_block" {
  bucket = aws_s3_bucket.flow_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# 3. Automated 3-Day Lifecycle Expiration Policy to Eliminate Cloud Costs
resource "aws_s3_bucket_lifecycle_configuration" "flow_logs_lifecycle" {
  bucket = aws_s3_bucket.flow_logs.id

  rule {
    id     = "AutoExpireLogs"
    status = "Enabled"

    filter {} # <--- Add this empty block to target the entire bucket cleanly

    expiration {
      days = 3 # Logs automatically delete after 72 hours
    }
  }
}

# 4. Mandatory AWS Log Delivery Service Bucket Permissions Policy
resource "aws_s3_bucket_policy" "flow_logs_policy" {
  bucket = aws_s3_bucket.flow_logs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AWSLogDeliveryWrite"
        Effect = "Allow"
        Principal = {
          Service = "delivery.logs.amazonaws.com"
        }
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.flow_logs.arn}/*"
        Condition = {
          StringEquals = {
            "s3:x-amz-acl" = "bucket-owner-full-control"
          }
        }
      },
      {
        Sid    = "AWSLogDeliveryCheck"
        Effect = "Allow"
        Principal = {
          Service = "delivery.logs.amazonaws.com"
        }
        Action   = "s3:GetBucketAcl"
        Resource = aws_s3_bucket.flow_logs.arn
      }
    ]
  })
}

# 5. Core VPC Flow Log Resource Mapping
resource "aws_flow_log" "main" {
  log_destination      = aws_s3_bucket.flow_logs.arn
  log_destination_type = "s3"
  traffic_type         = "ALL" # Captures both ACCEPT and REJECT packet streams
  vpc_id               = aws_vpc.main.id

  tags = { Name = "${var.project_name}-vpc-flow-logs" }
}
