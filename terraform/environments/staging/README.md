# Environment: `staging`

The composition root the CI/CD pipeline deploys. It wires the reusable modules
under [`../../modules`](../../modules) into the full multi-tier stack and is the
production-representative reference environment (Multi-AZ database, ASG headroom).

## Stack

```
network ──┬─> database (app SG → 5432)
          ├─> compute  (ALB + ASG + artifact bucket)   <── iam (instance profile)
          └─> observability (VPC flow logs)
```

## State

Remote S3 backend, key `staging/terraform.tfstate`. CI overrides the key per run:

```bash
terraform init -reconfigure -backend-config="key=ephemeral/terraform.tfstate"
```

## Deploy (mirrors the pipeline's two-phase pattern)

```bash
cd terraform/environments/staging
terraform init
terraform apply -target=module.compute.aws_s3_bucket.app_deploy -auto-approve
# ...stage app.jar / Dockerfile / docker-compose.yml into the bucket...
terraform apply -auto-approve
```

`db_username` has a default (`postgres`); the pipeline still passes
`-var="db_username=postgres"` explicitly.

## Outputs

`vpc_id`, `public_subnet_ids`, `private_subnet_ids`, `database_subnet_ids`,
`alb_dns_name`, `alb_arn`, `target_group_arn`, `app_deploy_bucket_name`,
`db_instance_endpoint`, `db_instance_address`.

See [`../../docs/terraform-module-usage.md`](../../../docs/terraform-module-usage.md)
for the module catalog.
