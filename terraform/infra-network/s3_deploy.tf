# Dynamic lookup to retrieve your current AWS Account ID for policies
data "aws_caller_identity" "current" {}

# Ephemeral deployment bucket to store the application payloads
resource "aws_s3_bucket" "app_deploy" {
  bucket        = "${var.project_name}-app-deploy-bucket-99"
  force_destroy = true # Mandatory to allow clean terraform destroy loops
  tags          = { Name = "${var.project_name}-app-deploy-bucket" }
}

# FIX CKV_AWS_145: Enforce Default Server-Side Encryption using AWS Managed KMS Keys
resource "aws_s3_bucket_server_side_encryption_configuration" "app_deploy_enc" {
  bucket = aws_s3_bucket.app_deploy.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
  }
}

# FIX CKV2_AWS_6: Enforce Strict Public Access Block boundaries on artifact storage
resource "aws_s3_bucket_public_access_block" "app_deploy_privacy" {
  bucket = aws_s3_bucket.app_deploy.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# FIX CKV2_AWS_61: Establish Data Lifecycle Expiration Mechanics
resource "aws_s3_bucket_lifecycle_configuration" "app_deploy_lifecycle" {
  bucket = aws_s3_bucket.app_deploy.id

  rule {
    id     = "AutoExpireArtifacts"
    status = "Enabled"

    filter {} # Targets all uploaded artifacts cleanly

    expiration {
      days = 1 # Ephemeral lifecycle: Purge payloads after 24 hours
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }
}

# Secure IAM Role allowing EC2 instances to assume an identity profile
resource "aws_iam_role" "ec2_s3_role" {
  name = "${var.project_name}-ec2-s3-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = { Service = "ec2.amazonaws.com" }
      }
    ]
  })
}

# Strict Read-Only Policy limited strictly to your app asset bucket
resource "aws_iam_role_policy" "ec2_s3_read_policy" {
  name = "${var.project_name}-ec2-s3-read-policy"
  role = aws_iam_role.ec2_s3_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["s3:GetObject", "s3:ListBucket"]
        Resource = [
          aws_s3_bucket.app_deploy.arn,
          "${aws_s3_bucket.app_deploy.arn}/*"
        ]
      }
    ]
  })
}

# The profile link that attaches directly to your compute instances
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.project_name}-ec2-instance-profile"
  role = aws_iam_role.ec2_s3_role.name
}
