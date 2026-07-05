# Module: `compute`

The application runtime tier: a public ALB fronting an Auto Scaling Group of
private, self-bootstrapping instances, plus the ephemeral S3 bucket that stages
the deployment artifacts.

## What it provisions

- **Artifact bucket** (`aws_s3_bucket.app_deploy`) — encrypted, private, lifecycle
  auto-expiry. The CI two-phase deploy targets this resource first.
- **ALB** (drop-invalid-header-fields) + HTTP:80 listener + target group with an
  `/actuator/health` health check.
- **Launch template** — Ubuntu 22.04, IMDSv2 required, no public IP, `user_data`
  that installs Docker, pulls the artifacts from S3, writes `/app/.env` from the
  RDS coordinates, and runs `docker compose up`.
- **Auto Scaling Group** across the private subnets with rolling instance refresh.

## The two-phase deploy contract

CI cannot reach the private subnets, so it stages artifacts through S3:

```bash
# Phase 1 — create ONLY the bucket
terraform apply -target=module.compute.aws_s3_bucket.app_deploy -auto-approve
# ...upload app.jar / Dockerfile / docker-compose.yml to it...
# Phase 2 — apply everything else (compute reads the artifacts on boot)
terraform apply -auto-approve
```

Because of this, `artifact_bucket_name` is passed **explicitly** and used verbatim
(no random suffix) so the address and name are stable for the pipeline.

## Usage

```hcl
module "compute" {
  source = "../../modules/compute"

  project_name          = "devsecops-p02"
  vpc_id                = module.network.vpc_id
  public_subnet_ids     = module.network.public_subnet_ids
  private_subnet_ids     = module.network.private_subnet_ids
  alb_security_group_id = module.network.alb_security_group_id
  app_security_group_id = module.network.app_security_group_id

  instance_profile_arn = module.iam.instance_profile_arn
  artifact_bucket_name = local.artifact_bucket_name

  db_address  = module.database.db_instance_address
  db_username = var.db_username
  db_password = module.database.db_password
}
```

## Key inputs

`instance_type` (`t2.micro`), `asg_min_size`/`asg_desired_capacity`/`asg_max_size`,
`artifact_expiration_days`, `health_check_path`, `app_port`.

## Outputs

`alb_dns_name`, `alb_arn`, `target_group_arn`, `app_deploy_bucket_name`,
`autoscaling_group_name`.
