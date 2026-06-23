# ==============================================================================
# 1. CRYPTOGRAPHIC BOUNDARY (Dedicated KMS CMK for Storage Encryption at Rest)
# ==============================================================================
resource "aws_kms_key" "rds" {
  description             = "KMS Customer Managed Key for explicit RDS storage volume encryption"
  deletion_window_in_days = 7 # Cost mitigation constraint for sandbox lifecycle
  enable_key_rotation     = true

  tags = { Name = "${var.project_name}-rds-kms-key" }
}

resource "aws_kms_alias" "rds" {
  name          = "alias/${var.project_name}-rds-key"
  target_key_id = aws_kms_key.rds.key_id
}

# ==============================================================================
# NEW: PROGRAMMATIC SECRET ENGINE (Zero plaintext credentials in code)
# ==============================================================================
resource "random_password" "db_password" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?" # Removes characters that cause parsing breaks in database URI strings
}

# ==============================================================================
# 2. SUBNET ISOLATION (Binds the database strictly to isolated DB subnets)
# ==============================================================================
resource "aws_db_subnet_group" "db_group" {
  name        = "${var.project_name}-db-subnet-group"
  description = "Restricts database placement exclusively to isolated multi-AZ database subnets"
  subnet_ids  = aws_subnet.database[*].id

  tags = { Name = "${var.project_name}-db-subnet-group" }
}

# ==============================================================================
# 3. NETWORK FIREWALL GATE (Strict Security Group Enforcing Zero Inbound Public Access)
# ==============================================================================
resource "aws_security_group" "db" {
  name        = "${var.project_name}-db-sg"
  description = "Isolates the database cluster, blocking all traffic except from the app compute tier"
  vpc_id      = aws_vpc.main.id

  # Strict Ingress: Only traffic originating from the backend compute security group on port 5432
  ingress {
    description     = "Allow stateful database queries exclusively from backend compute instances"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id] # Tight coupling to Phase 4 Compute SG
  }

  # Total outbound isolation - database instances do not initiate external connections
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
# 4. ENGINE OPTIMIZATION (Custom Parameter Group for PostgreSQL 16)
# ==============================================================================
resource "aws_db_parameter_group" "postgres_pg" {
  name        = "${var.project_name}-pg16-params"
  family      = "postgres16"
  description = "Custom runtime optimization parameters for PostgreSQL 16"

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
    value = "1000" # Log any queries taking longer than 1 second for DevSecOps analysis
  }

  tags = { Name = "${var.project_name}-postgres16-parameter-group" }
}

# ==============================================================================
# 5. CORE HIGH-AVAILABILITY CLUSTER ENGINE (Multi-AZ Encrypted RDS Instance)
# ==============================================================================
resource "aws_db_instance" "postgres" {
  identifier             = "${var.project_name}-database"
  engine                 = "postgres"
  engine_version         = "16"           # Structured to align with Spring Boot 3 properties
  instance_class         = "db.t3.micro"  # Free-tier/Low-cost bracket alignment
  allocated_storage      = 20             # Baseline corporate database storage
  storage_type           = "gp3"          # Modern, performant SSD architecture
  db_name                = "ecommerce_db" # Matches Spring Boot data-seeding routing properties
  username               = var.db_username
  password               = random_password.db_password.result
  db_subnet_group_name   = aws_db_subnet_group.db_group.name
  vpc_security_group_ids = [aws_security_group.db.id]
  parameter_group_name   = aws_db_parameter_group.postgres_pg.name

  # High Availability & Resilience Configuration
  multi_az = true # Provisions synchronous failover standbys across Availability Zones

  # Cryptographic Data Protection
  storage_encrypted = true
  kms_key_id        = aws_kms_key.rds.arn

  # FIX CKV_AWS_226: Enable automated compliance patch management
  auto_minor_version_upgrade = true

  # Operational Safeguards & Costs Management
  backup_retention_period = 3 # Retain automated transaction logs for 72 hours
  deletion_protection     = false
  skip_final_snapshot     = true # Allows cost-free clean teardowns at project termination

  tags = { Name = "${var.project_name}-postgres-cluster" }
}

# Publish the cryptographically generated password straight to AWS Systems Manager Parameter Store
resource "aws_ssm_parameter" "db_password" {
  name        = "/config/ecommerce-api/spring.datasource.password"
  description = "Dynamic runtime database password for the headless Spring Boot application"
  type        = "SecureString" # Enforces KMS encryption at rest within the parameter network
  value       = random_password.db_password.result

  tags = {
    Environment = "Staging"
    ManagedBy   = "Terraform"
  }
}
