# 1. Retrieve the latest canonical Ubuntu 22.04 LTS AMI mapping for eu-west-3
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical ID

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

# 2. Immutable EC2 Template Blueprint
resource "aws_launch_template" "app_template" {
  name_prefix   = "${var.project_name}-template-"
  image_id      = data.aws_ami.ubuntu.id
  instance_type = "t2.micro" # Free-tier constraint enforced for cost-mitigation

  # FIX CKV_AWS_79: Enforce modern IMDSv2 token validation mechanics
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required" # Blocks legacy IMDSv1 calls
    http_put_response_hop_limit = 1
  }

  network_interfaces {
    associate_public_ip_address = false # Strict zero-internet footprint inside the private tier
    security_groups             = [aws_security_group.app.id]
  }

  # Automated Workstation Bootstrapping Script
  user_data = base64encode(<<-EOF
              #!/bin/bash
              apt-get update -y
              apt-get install -y apt-transport-https ca-certificates curl software-properties-common
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
              add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu jammy stable"
              apt-get update -y
              apt-get install -y docker-ce docker-compose-plugin
              systemctl enable docker
              systemctl start docker
              EOF
  )

  lifecycle {
    create_before_destroy = true
  }

  tags = { Name = "${var.project_name}-launch-template" }
}

# 3. High-Availability Auto Scaling Group
resource "aws_autoscaling_group" "app_asg" {
  # Standard resource identifier mapping
  name                = "${var.project_name}-asg"
  desired_capacity    = 1 # Maintained at minimum baseline for cost optimization
  min_size            = 1
  max_size            = 2 # Scalable bursting cap limit
  target_group_arns   = [aws_lb_target_group.app_tg.arn]
  vpc_zone_identifier = aws_subnet.private[*].id # Spreads compute across multiple private availability zones

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
