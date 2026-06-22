variable "aws_region" {
  type        = string
  description = "The target AWS Region for backend infrastructure orchestration"
  default     = "eu-west-3" # Paris proximity for low latency and compliance
}

variable "project_name" {
  type        = string
  description = "Project prefix utilized for resource structural naming convention"
  default     = "devsecops-p02"
}
