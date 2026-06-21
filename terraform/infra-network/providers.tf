terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }

  backend "s3" {
    bucket       = "devsecops-p02-remote-state-storage-99"
    key          = "staging/terraform.tfstate" # Isolated state key for the application runtime layer
    region       = "eu-west-3"
    encrypt      = true
    use_lockfile = true # Leveraging our zero-cost native S3 locking engine
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "DevSecOps-Project02"
      Environment = "Staging"
      ManagedBy   = "Terraform"
    }
  }
}
