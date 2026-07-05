# Staging environment values.
# Production-representative: Multi-AZ database, ASG headroom for bursting.
project_name = "devsecops-p02"
aws_region   = "eu-west-3"

vpc_cidr              = "10.0.0.0/16"
public_subnet_cidrs   = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs  = ["10.0.11.0/24", "10.0.12.0/24"]
database_subnet_cidrs = ["10.0.21.0/24", "10.0.22.0/24"]
availability_zones    = ["eu-west-3a", "eu-west-3b"]

db_multi_az = true

instance_type        = "t2.micro"
asg_min_size         = 1
asg_desired_capacity = 1
asg_max_size         = 2
