variable "project_name" {
  type        = string
  description = "Project name prefix applied to observability resources"
}

variable "vpc_id" {
  type        = string
  description = "VPC whose traffic is captured by the flow logs"
}

variable "flow_logs_bucket_name" {
  type        = string
  description = "Exact name of the S3 bucket that stores VPC flow logs"
}

variable "log_retention_days" {
  type        = number
  description = "Days before flow log objects auto-expire"
  default     = 3
}

variable "traffic_type" {
  type        = string
  description = "Which packets to capture: ACCEPT, REJECT or ALL"
  default     = "ALL"
}
