# ==============================================================================
# EPHEMERAL DEPLOYMENT ARTIFACT BUCKET
# The CI two-phase deploy targets `module.compute.aws_s3_bucket.app_deploy`
# to create this bucket, uploads app.jar/Dockerfile/compose, then applies the rest.
# ==============================================================================
resource "aws_s3_bucket" "app_deploy" {
  bucket        = var.artifact_bucket_name
  force_destroy = true # Mandatory for clean terraform destroy loops
  tags          = { Name = "${var.project_name}-app-deploy-bucket" }
}

# CKV_AWS_145: default server-side encryption with KMS.
resource "aws_s3_bucket_server_side_encryption_configuration" "app_deploy_enc" {
  bucket = aws_s3_bucket.app_deploy.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
  }
}

# CKV2_AWS_6: strict public access block on artifact storage.
resource "aws_s3_bucket_public_access_block" "app_deploy_privacy" {
  bucket = aws_s3_bucket.app_deploy.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CKV2_AWS_61: lifecycle expiration — ephemeral artifact hygiene.
resource "aws_s3_bucket_lifecycle_configuration" "app_deploy_lifecycle" {
  bucket = aws_s3_bucket.app_deploy.id

  rule {
    id     = "AutoExpireArtifacts"
    status = "Enabled"

    filter {}

    expiration {
      days = var.artifact_expiration_days
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }
}

# ==============================================================================
# APPLICATION LOAD BALANCER
# ==============================================================================
resource "aws_lb" "external" {
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids

  # CKV_AWS_131: drop non-conforming header injections.
  drop_invalid_header_fields = true

  tags = { Name = "${var.project_name}-alb" }
}

resource "aws_lb_target_group" "app_tg" {
  name        = "${var.project_name}-app-tg"
  port        = var.app_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "instance"

  health_check {
    enabled             = true
    path                = var.health_check_path
    port                = tostring(var.app_port)
    protocol            = "HTTP"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200"
  }

  tags = { Name = "${var.project_name}-target-group" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.external.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app_tg.arn
  }
}

# ==============================================================================
# COMPUTE — LAUNCH TEMPLATE + AUTO SCALING GROUP
# ==============================================================================
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

resource "aws_launch_template" "app_template" {
  name_prefix   = "${var.project_name}-template-"
  image_id      = data.aws_ami.ubuntu.id
  instance_type = var.instance_type

  iam_instance_profile {
    arn = var.instance_profile_arn
  }

  # CKV_AWS_79: enforce IMDSv2.
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  network_interfaces {
    associate_public_ip_address = false # Zero-internet footprint inside the private tier
    security_groups             = [var.app_security_group_id]
  }

  user_data = base64encode(<<-EOF
              #!/bin/bash
              exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/null) 2>&1

              apt-get update -y
              apt-get install -y apt-transport-https ca-certificates curl software-properties-common unzip
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
              add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu jammy stable"
              apt-get update -y
              apt-get install -y docker-ce docker-compose-plugin
              systemctl enable docker
              systemctl start docker

              # Install AWS CLI v2
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
              unzip awscliv2.zip
              ./aws/install

              # Establish runtime layout directories
              mkdir -p /app
              cd /app

              # Download the pre-vetted compliance payloads from the ephemeral S3 asset bucket
              aws s3 cp s3://${aws_s3_bucket.app_deploy.bucket}/app.jar .
              aws s3 cp s3://${aws_s3_bucket.app_deploy.bucket}/Dockerfile .
              aws s3 cp s3://${aws_s3_bucket.app_deploy.bucket}/docker-compose.yml .

              # ==============================================================================
              # CONFIGURATION BRIDGE: write cloud variables to a local .env
              # Single quotes: Terraform interpolates before bash runs, so the shell
              # must NOT expand anything inside the substituted values ($, {}, etc.).
              # ==============================================================================
              echo 'RDS_ENDPOINT=${var.db_address}' > /app/.env
              echo 'DB_USERNAME=${var.db_username}' >> /app/.env
              echo 'DB_PASSWORD=${var.db_password}' >> /app/.env

              # Launch the Spring Boot container runtime
              docker compose up --build -d
              EOF
  )

  lifecycle {
    create_before_destroy = true
  }

  tags = { Name = "${var.project_name}-launch-template" }
}

resource "aws_autoscaling_group" "app_asg" {
  name                = "${var.project_name}-asg"
  desired_capacity    = var.asg_desired_capacity
  min_size            = var.asg_min_size
  max_size            = var.asg_max_size
  target_group_arns   = [aws_lb_target_group.app_tg.arn]
  vpc_zone_identifier = var.private_subnet_ids

  launch_template {
    id      = aws_launch_template.app_template.id
    version = "$Latest"
  }

  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 50
    }
    triggers = ["tag"]
  }

  lifecycle {
    create_before_destroy = true
  }
}
