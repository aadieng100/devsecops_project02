terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
  backend "s3" {
    bucket       = "devsecops-p02-remote-state-storage-99"
    key          = "bootstrap/terraform.tfstate"
    region       = "eu-west-3"
    encrypt      = true
    use_lockfile = true # Enforces native S3 cryptographic locking, bypassing DynamoDB completely
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = "DevSecOps-Project02"
      ManagedBy = "Terraform"
      Layer     = "Bootstrap-Backend"
    }
  }
}
