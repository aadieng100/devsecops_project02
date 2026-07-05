# Environment: `dev`

A low-cost twin of `staging` built from the **same modules**, tuned for iteration
speed and minimal spend. This is the self-service sandbox a developer stands up to
try a change before it hits the pipeline.

## Differences from staging

| Dimension | dev | staging |
|---|---|---|
| `project_name` | `devsecops-p02-dev` (fully namespaced) | `devsecops-p02` |
| VPC CIDR | `10.10.0.0/16` | `10.0.0.0/16` |
| Database Multi-AZ | **off** | on |
| ASG max size | `1` | `2` |
| Flow-log retention | 1 day | 3 days |
| SSM secret path | `/config/ecommerce-api/dev/...` | `/config/ecommerce-api/...` |
| State key | `dev/terraform.tfstate` | `staging/terraform.tfstate` |

Because every resource name derives from `project_name`, dev and staging can live
side-by-side in the same AWS account without collisions.

## Deploy

```bash
cd terraform/environments/dev
terraform init
terraform plan     # tfvars are picked up automatically
terraform apply
```

See [`../../docs/developer-self-service-guide.md`](../../../docs/developer-self-service-guide.md).
