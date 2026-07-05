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
  description = "Foundational CIDR block for the network topology"
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks for public ingress subnets"
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks for private compute subnets"
  default     = ["10.0.11.0/24", "10.0.12.0/24"]
}

variable "database_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks for isolated database subnets"
  default     = ["10.0.21.0/24", "10.0.22.0/24"]
}

variable "availability_zones" {
  type        = list(string)
  description = "Target AWS availability zones"
  default     = ["eu-west-3a", "eu-west-3b"]
}

variable "db_username" {
  type        = string
  description = "Master administrative username for the PostgreSQL engine"
  default     = "postgres"
}

variable "db_multi_az" {
  type        = bool
  description = "Provision a Multi-AZ standby for the database"
  default     = true
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type for the application tier"
  default     = "t2.micro"
}

variable "asg_min_size" {
  type        = number
  description = "Minimum ASG size"
  default     = 1
}

variable "asg_desired_capacity" {
  type        = number
  description = "Desired ASG capacity"
  default     = 1
}

variable "asg_max_size" {
  type        = number
  description = "Maximum ASG size"
  default     = 2
}
