variable "project_name" {
  type        = string
  description = "Project name prefix applied to IAM resources"
}

variable "artifact_bucket_name" {
  type        = string
  description = "Name of the deployment artifact bucket the EC2 role is granted read access to. Passed by name (not by resource reference) so this module has no dependency cycle with the compute module that creates the bucket."
}
