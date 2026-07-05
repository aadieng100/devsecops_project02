variable "project_name" {
  type        = string
  description = "Project name prefix applied to compute resources"
}

variable "vpc_id" {
  type        = string
  description = "VPC the target group / ALB live in"
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnet IDs the ALB is attached to"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs the Auto Scaling Group spreads instances across"
}

variable "alb_security_group_id" {
  type        = string
  description = "Security group ID for the ALB"
}

variable "app_security_group_id" {
  type        = string
  description = "Security group ID for the application instances"
}

variable "instance_profile_arn" {
  type        = string
  description = "ARN of the EC2 instance profile (from the iam module)"
}

variable "artifact_bucket_name" {
  type        = string
  description = "Exact name of the ephemeral deployment artifact bucket to create. The CI two-phase deploy targets module.compute.aws_s3_bucket.app_deploy before uploading here."
}

variable "app_port" {
  type        = number
  description = "Application listen port behind the ALB"
  default     = 8080
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type"
  default     = "t2.micro"
}

variable "asg_min_size" {
  type        = number
  description = "Minimum Auto Scaling Group size"
  default     = 1
}

variable "asg_desired_capacity" {
  type        = number
  description = "Desired Auto Scaling Group capacity"
  default     = 1
}

variable "asg_max_size" {
  type        = number
  description = "Maximum Auto Scaling Group size"
  default     = 2
}

variable "artifact_expiration_days" {
  type        = number
  description = "Days before staged artifacts in the deploy bucket auto-expire"
  default     = 1
}

variable "health_check_path" {
  type        = string
  description = "Target group HTTP health check path"
  default     = "/actuator/health"
}

# ------- consumed only by the instance bootstrap (user_data) -------
variable "db_address" {
  type        = string
  description = "RDS DNS address wired into the instance .env"
}

variable "db_username" {
  type        = string
  description = "Database username wired into the instance .env"
}

variable "db_password" {
  type        = string
  description = "Database password wired into the instance .env"
  sensitive   = true
}
