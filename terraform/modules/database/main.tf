data "aws_caller_identity" "current" {}

# ==============================================================================
# CRYPTOGRAPHIC BOUNDARY (dedicated KMS CMK for storage encryption at rest)
# ==============================================================================
resource "aws_kms_key" "rds" {
  description             = "KMS Customer Managed Key for explicit RDS storage volume encryption"
  deletion_window_in_days = var.kms_deletion_window_in_days
  enable_key_rotation     = true

  # CKV2_AWS_64: explicit key policy allowing account root administration.
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      }
    ]
  })

  tags = { Name = "${var.project_name}-rds-kms-key" }
}

resource "aws_kms_alias" "rds" {
  name          = "alias/${var.project_name}-rds-key"
  target_key_id = aws_kms_key.rds.key_id
}

# ==============================================================================
# PROGRAMMATIC SECRET ENGINE (zero plaintext credentials in code)
# ==============================================================================
resource "random_password" "db_password" {
  length  = 24
  special = true
  # Excludes chars that break DB URI parsing, PLUS `$`: the compute bootstrap
  # writes the password into /app/.env via a double-quoted shell echo and Docker
  # Compose interpolates `$` in .env values — a `$` in the password gets expanded
  # at both layers, mangling the credential and crashing the app on boot (~25%
  # of runs, since random_password rolls it in 24 chars).
  override_special = "!#%&*()-_=+[]{}<>:?"
}

# ==============================================================================
# SUBNET ISOLATION
# ==============================================================================
resource "aws_db_subnet_group" "db_group" {
  name        = "${var.project_name}-db-subnet-group"
  description = "Restricts database placement to isolated multi-AZ database subnets"
  subnet_ids  = var.database_subnet_ids

  tags = { Name = "${var.project_name}-db-subnet-group" }
}

# ==============================================================================
# NETWORK FIREWALL GATE (zero inbound public access; app tier only)
# ==============================================================================
resource "aws_security_group" "db" {
  name        = "${var.project_name}-db-sg"
  description = "Isolates the database, blocking all traffic except from the app compute tier"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Allow stateful database queries exclusively from backend compute instances"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.app_security_group_id]
  }

  egress {
    description = "Allow responses back out to authorized connections"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-db-sg" }
}

# ==============================================================================
# ENGINE OPTIMIZATION (custom parameter group)
# ==============================================================================
resource "aws_db_parameter_group" "postgres_pg" {
  name        = "${var.project_name}-pg-params"
  family      = var.parameter_group_family
  description = "Custom runtime optimization parameters for PostgreSQL"

  parameter {
    name  = "log_connections"
    value = "1"
  }

  parameter {
    name  = "log_disconnections"
    value = "1"
  }

  parameter {
    name  = "log_min_duration_statement"
    value = "1000" # Log queries slower than 1s for DevSecOps analysis
  }

  tags = { Name = "${var.project_name}-postgres-parameter-group" }
}

# ==============================================================================
# CORE RDS CLUSTER ENGINE
# ==============================================================================
resource "aws_db_instance" "postgres" {
  identifier             = "${var.project_name}-database"
  engine                 = "postgres"
  engine_version         = var.engine_version
  instance_class         = var.instance_class
  allocated_storage      = var.allocated_storage
  storage_type           = "gp3"
  db_name                = var.db_name
  username               = var.db_username
  password               = random_password.db_password.result
  db_subnet_group_name   = aws_db_subnet_group.db_group.name
  vpc_security_group_ids = [aws_security_group.db.id]
  parameter_group_name   = aws_db_parameter_group.postgres_pg.name

  multi_az = var.multi_az

  storage_encrypted = true
  kms_key_id        = aws_kms_key.rds.arn

  # CKV_AWS_226: automated compliance patch management.
  auto_minor_version_upgrade = true
  # CKV_AWS_161: IAM database authentication.
  iam_database_authentication_enabled = true

  backup_retention_period = var.backup_retention_period
  deletion_protection     = var.deletion_protection
  skip_final_snapshot     = true

  tags = { Name = "${var.project_name}-postgres-cluster" }
}

# Publish the generated password to SSM Parameter Store (KMS-encrypted at rest).
resource "aws_ssm_parameter" "db_password" {
  name        = var.ssm_password_parameter_name
  description = "Dynamic runtime database password for the headless Spring Boot application"
  type        = "SecureString"
  value       = random_password.db_password.result

  tags = { ManagedBy = "Terraform" }
}
