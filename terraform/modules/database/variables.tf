variable "project_name" {
  type        = string
  description = "Project name prefix applied to database resources"
}

variable "vpc_id" {
  type        = string
  description = "VPC the database security group is created in"
}

variable "database_subnet_ids" {
  type        = list(string)
  description = "Isolated subnet IDs the RDS instance is placed in (needs >= 2 for Multi-AZ)"
}

variable "app_security_group_id" {
  type        = string
  description = "Security group ID of the app tier permitted to reach the database on 5432"
}

variable "db_username" {
  type        = string
  description = "Master administrative username for the PostgreSQL engine"
  default     = "postgres"
}

variable "db_name" {
  type        = string
  description = "Initial database name created on the instance"
  default     = "ecommerce_db"
}

variable "engine_version" {
  type        = string
  description = "PostgreSQL engine major version"
  default     = "16"
}

variable "parameter_group_family" {
  type        = string
  description = "RDS parameter group family (must match engine_version)"
  default     = "postgres16"
}

variable "instance_class" {
  type        = string
  description = "RDS instance class"
  default     = "db.t3.micro"
}

variable "allocated_storage" {
  type        = number
  description = "Allocated storage in GB"
  default     = 20
}

variable "multi_az" {
  type        = bool
  description = "Provision a synchronous standby across AZs. Enable for staging/prod, disable for cheap dev."
  default     = true
}

variable "backup_retention_period" {
  type        = number
  description = "Days of automated backups to retain"
  default     = 3
}

variable "deletion_protection" {
  type        = bool
  description = "Guard the instance against accidental deletion"
  default     = false
}

variable "ssm_password_parameter_name" {
  type        = string
  description = "SSM Parameter Store path where the generated DB password is published"
  default     = "/config/ecommerce-api/spring.datasource.password"
}

variable "kms_deletion_window_in_days" {
  type        = number
  description = "Waiting period before the RDS CMK is deleted"
  default     = 7
}
