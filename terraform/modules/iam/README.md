# Module: `iam`

Workload identity for the compute tier: an EC2 role scoped to **read-only** access
on the deployment artifact bucket, plus the instance profile that binds it.

## Design note — why `artifact_bucket_name`, not a bucket resource?

The compute module creates the artifact bucket **and** consumes the instance
profile. If this module referenced `module.compute.bucket_arn` and compute
referenced `module.iam.instance_profile_arn`, Terraform would detect a dependency
cycle. Instead the environment passes the bucket **name** (a deterministic string)
and this module rebuilds the ARN locally. No cycle, no `depends_on` hacks.

## Usage

```hcl
module "iam" {
  source = "../../modules/iam"

  project_name         = "devsecops-p02"
  artifact_bucket_name = "devsecops-p02-app-deploy-bucket-99"
}
```

## Inputs

| Name | Type | Description |
|---|---|---|
| `project_name` | string | Prefix applied to IAM resource names |
| `artifact_bucket_name` | string | Bucket the role gets `s3:GetObject`/`s3:ListBucket` on |

## Outputs

`instance_profile_arn`, `instance_profile_name`, `role_name`.
