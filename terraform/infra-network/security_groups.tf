# 1. Edge Load Balancer Security Group
resource "aws_security_group" "alb" {
  name        = "${var.project_name}-alb-sg"
  description = "Enforces perimeter security control for public ingress traffic"
  vpc_id      = aws_vpc.main.id

  # Public Ingress HTTP
  ingress {
    description = "Allow cleartext HTTP traffic from the public internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Total Egress Freedom
  egress {
    description = "Allow full outbound traffic routing"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-alb-sg" }
}

# 2. Application Compute Security Group
resource "aws_security_group" "app" {
  name        = "${var.project_name}-app-sg"
  description = "Shields backend application computing units from unvetted networks"
  vpc_id      = aws_vpc.main.id

  # Restricted Ingress: Only from the ALB on the Spring Boot target port
  ingress {
    description     = "Isolate ingress exclusively to backend reverse-proxy requests from the ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Outbound patching access enabled exclusively via the NAT Gateway
  egress {
    description = "Allow outbound communication for system updates and dependency downloads"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-app-sg" }
}
