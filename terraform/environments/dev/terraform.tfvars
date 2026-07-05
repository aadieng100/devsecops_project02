# Dev environment values.
# Cost-optimized: single-AZ database, no ASG bursting, short log retention.
project_name = "devsecops-p02-dev"
aws_region   = "eu-west-3"

vpc_cidr              = "10.10.0.0/16"
public_subnet_cidrs   = ["10.10.1.0/24", "10.10.2.0/24"]
private_subnet_cidrs  = ["10.10.11.0/24", "10.10.12.0/24"]
database_subnet_cidrs = ["10.10.21.0/24", "10.10.22.0/24"]
availability_zones    = ["eu-west-3a", "eu-west-3b"]

db_multi_az = false

instance_type        = "t2.micro"
asg_min_size         = 1
asg_desired_capacity = 1
asg_max_size         = 1
