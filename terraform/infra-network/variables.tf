variable "aws_region" {
  type        = string
  description = "Target deployment AWS region"
  default     = "eu-west-3"
}

variable "project_name" {
  type        = string
  description = "Project name prefix applied to infrastructure elements"
  default     = "devsecops-p02"
}

variable "vpc_cidr" {
  type        = string
  description = "The foundational CIDR block for the isolated network topology"
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks allocated to public ingress network layers"
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks allocated to secure compute application layers"
  default     = ["10.0.11.0/24", "10.0.12.0/24"]
}

variable "database_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks allocated to isolated storage persistence layers"
  default     = ["10.0.21.0/24", "10.0.22.0/24"]
}

variable "availability_zones" {
  type        = list(string)
  description = "Target AWS availability zones for multi-AZ disaster recovery"
  default     = ["eu-west-3a", "eu-west-3b"]
}

variable "db_username" {
  type        = string
  description = "The master administrative username for the PostgreSQL engine"
  default     = "postgres"
}
