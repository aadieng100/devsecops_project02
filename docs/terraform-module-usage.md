# Terraform Module Usage Guide

> The IaC pattern library for **DevSecOps-Project02**. Compose environments from
> small, single-responsibility, security-hardened modules instead of copy-pasting
> monolithic `.tf` files.

## Layout

```
terraform/
├── bootstrap/              # One-time backend: state bucket, KMS, GitHub OIDC role
├── modules/               # Reusable pattern library (the building blocks)
│   ├── network/           # VPC, subnets, routing, NAT, edge security groups
│   ├── iam/               # EC2 instance role + profile (least-privilege S3 read)
│   ├── database/          # Encrypted Multi-AZ RDS PostgreSQL + generated secret
│   ├── compute/           # ALB + Auto Scaling Group + ephemeral artifact bucket
│   └── observability/     # VPC flow logs → locked-down S3
└── environments/          # Composition roots (one state file each)
    ├── staging/           # Production-representative; deployed by CI
    └── dev/               # Low-cost sandbox twin
```

**Golden rule:** modules never contain a `backend`, a `provider`, or hardcoded
account IDs. Environments own state and provider config; modules own resources.

## Module dependency graph

```
                 ┌─────────────┐
                 │   network   │  vpc_id, subnet_ids, alb_sg, app_sg
                 └──────┬──────┘
        ┌───────────────┼────────────────────────┐
        ▼               ▼                         ▼
  ┌───────────┐   ┌───────────┐            ┌────────────────┐
  │  database  │   │    iam    │            │ observability  │
  │ (app_sg →  │   │ (bucket   │            │ (vpc flow logs)│
  │   5432)    │   │  name)    │            └────────────────┘
  └─────┬─────┘   └─────┬─────┘
        │ db_address    │ instance_profile_arn
        │ db_password   │
        ▼               ▼
        ┌───────────────────────┐
        │        compute         │  alb_dns_name, target_group_arn,
        │  ALB + ASG + artifact  │  app_deploy_bucket_name
        │        bucket          │
        └───────────────────────┘
```

### Breaking the `iam` ⇄ `compute` cycle

`compute` creates the artifact bucket **and** needs the instance profile; `iam`
needs the bucket ARN to scope its read policy. Referencing each other's outputs
would be a cycle. Instead the environment computes the bucket **name** once as a
local and passes that string to both modules — `iam` rebuilds the ARN from the
name. See `terraform/environments/staging/main.tf`:

```hcl
locals {
  artifact_bucket_name = "${var.project_name}-app-deploy-bucket-99"
}

module "iam" {
  source               = "../../modules/iam"
  artifact_bucket_name = local.artifact_bucket_name   # name only
}

module "compute" {
  source               = "../../modules/compute"
  instance_profile_arn = module.iam.instance_profile_arn
  artifact_bucket_name = local.artifact_bucket_name   # same name, creates the bucket
}
```

## Module reference

Each module has its own `README.md` with the full input/output table. Quick index:

| Module | Purpose | Key inputs | Key outputs |
|---|---|---|---|
| [`network`](../terraform/modules/network/README.md) | VPC + 3 subnet tiers + SGs | `vpc_cidr`, `*_subnet_cidrs`, `availability_zones` | `vpc_id`, `*_subnet_ids`, `alb_security_group_id`, `app_security_group_id` |
| [`iam`](../terraform/modules/iam/README.md) | EC2 role + instance profile | `artifact_bucket_name` | `instance_profile_arn` |
| [`database`](../terraform/modules/database/README.md) | RDS PostgreSQL + secret | `app_security_group_id`, `multi_az` | `db_instance_address`, `db_password` (sensitive) |
| [`compute`](../terraform/modules/compute/README.md) | ALB + ASG + artifact bucket | `instance_profile_arn`, `db_address`, `db_password` | `alb_dns_name`, `target_group_arn`, `app_deploy_bucket_name` |
| [`observability`](../terraform/modules/observability/README.md) | VPC flow logs | `flow_logs_bucket_name` | `flow_logs_bucket_name` |

## Consuming a module

```hcl
module "network" {
  source = "../../modules/network"   # relative path from an environment

  project_name       = var.project_name
  availability_zones = ["eu-west-3a", "eu-west-3b"]
}
```

- **Source paths are relative** to the calling environment (`../../modules/<name>`).
- After adding or changing a module block, run `terraform init` to (re)install it.
- Every module pins its own `required_providers` in `versions.tf`; Terraform merges
  these with the environment's.

## Adding a new environment (e.g. `prod`)

1. `cp -r terraform/environments/staging terraform/environments/prod`
2. In `providers.tf`, change the backend `key` to `prod/terraform.tfstate` and the
   `Environment` default tag to `Prod`.
3. In `terraform.tfvars`, set a unique `project_name` / VPC CIDR and prod sizing
   (`db_multi_az = true`, larger `asg_max_size`, etc.).
4. `terraform init && terraform plan`.

That's it — no module edits. New environments are configuration, not code.

## Adding a new module

1. `mkdir terraform/modules/<name>` with `main.tf`, `variables.tf`, `outputs.tf`,
   `versions.tf`, `README.md`.
2. No `provider`/`backend` blocks. Take everything environment-specific as a
   variable; expose everything downstream needs as an output.
3. Wire it into an environment's `main.tf` and `terraform init`.

## Conventions checklist

- [ ] Resource names derive from `var.project_name` (enables side-by-side envs).
- [ ] No secrets in code — generate with `random_password`, publish to SSM.
- [ ] Checkov-clean, or the finding is justified in the environment's `skip_check`.
- [ ] `terraform fmt -recursive` before commit; `terraform validate` per environment.
- [ ] Sensitive outputs marked `sensitive = true`.
