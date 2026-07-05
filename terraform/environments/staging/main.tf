# ==============================================================================
# STAGING ENVIRONMENT — composition root
# Wires the reusable modules under ../../modules into the full multi-tier stack.
# ==============================================================================

locals {
  # Deterministic, globally-unique bucket names. The compute artifact bucket name
  # is shared with the iam module (to build the read policy ARN) WITHOUT creating a
  # module dependency cycle — both consume the string, only compute creates it.
  artifact_bucket_name  = "${var.project_name}-app-deploy-bucket-99"
  flow_logs_bucket_name = "${var.project_name}-vpc-flow-logs-99"
}

module "network" {
  source = "../../modules/network"

  project_name          = var.project_name
  vpc_cidr              = var.vpc_cidr
  public_subnet_cidrs   = var.public_subnet_cidrs
  private_subnet_cidrs  = var.private_subnet_cidrs
  database_subnet_cidrs = var.database_subnet_cidrs
  availability_zones    = var.availability_zones
}

module "iam" {
  source = "../../modules/iam"

  project_name         = var.project_name
  artifact_bucket_name = local.artifact_bucket_name
}

module "database" {
  source = "../../modules/database"

  project_name          = var.project_name
  vpc_id                = module.network.vpc_id
  database_subnet_ids   = module.network.database_subnet_ids
  app_security_group_id = module.network.app_security_group_id

  db_username = var.db_username
  multi_az    = var.db_multi_az
}

module "compute" {
  source = "../../modules/compute"

  project_name          = var.project_name
  vpc_id                = module.network.vpc_id
  public_subnet_ids     = module.network.public_subnet_ids
  private_subnet_ids    = module.network.private_subnet_ids
  alb_security_group_id = module.network.alb_security_group_id
  app_security_group_id = module.network.app_security_group_id

  instance_profile_arn = module.iam.instance_profile_arn
  artifact_bucket_name = local.artifact_bucket_name

  instance_type        = var.instance_type
  asg_min_size         = var.asg_min_size
  asg_desired_capacity = var.asg_desired_capacity
  asg_max_size         = var.asg_max_size

  db_address  = module.database.db_instance_address
  db_username = var.db_username
  db_password = module.database.db_password
}

module "observability" {
  source = "../../modules/observability"

  project_name          = var.project_name
  vpc_id                = module.network.vpc_id
  flow_logs_bucket_name = local.flow_logs_bucket_name
}
