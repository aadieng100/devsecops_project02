# ==============================================================================
# DEV ENVIRONMENT — composition root
# Same module wiring as staging, tuned for low cost (single-AZ DB, no ASG burst)
# and fully namespaced (project_name = "devsecops-p02-dev") so it can coexist
# with staging in the same account.
# ==============================================================================

locals {
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

  # Dev-scoped SSM path so it never clashes with the staging secret.
  ssm_password_parameter_name = "/config/ecommerce-api/dev/spring.datasource.password"
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
  log_retention_days    = 1
}
