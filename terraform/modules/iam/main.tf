locals {
  # Derive the bucket ARN from its deterministic name so this module never has to
  # reference the compute module's aws_s3_bucket resource (which would create a cycle).
  artifact_bucket_arn = "arn:aws:s3:::${var.artifact_bucket_name}"
}

# Secure IAM role allowing EC2 instances to assume an identity profile.
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

  tags = { Name = "${var.project_name}-ec2-s3-role" }
}

# Least-privilege read-only policy limited strictly to the app artifact bucket.
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
          local.artifact_bucket_arn,
          "${local.artifact_bucket_arn}/*"
        ]
      }
    ]
  })
}

# Instance profile that attaches the role to compute instances.
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.project_name}-ec2-instance-profile"
  role = aws_iam_role.ec2_s3_role.name
}
