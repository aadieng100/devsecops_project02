# Module: `database`

Encrypted, network-isolated PostgreSQL on RDS with a generated credential that
never touches source control.

## What it provisions

- Dedicated KMS CMK (rotation on) + alias for storage encryption at rest
- `random_password` (24 chars) — the master credential, generated at apply time
- DB subnet group pinned to the isolated database subnets
- Security group allowing `5432` **only** from the app tier security group
- Custom parameter group (connection + slow-query logging)
- The RDS instance itself (Multi-AZ optional, `gp3`, IAM auth, auto minor upgrades)
- The password mirrored into SSM Parameter Store as a `SecureString`

## Usage

```hcl
module "database" {
  source = "../../modules/database"

  project_name          = "devsecops-p02"
  vpc_id                = module.network.vpc_id
  database_subnet_ids   = module.network.database_subnet_ids
  app_security_group_id = module.network.app_security_group_id

  db_username = "postgres"
  multi_az    = true            # false for cheap dev
}
```

## Key inputs

| Name | Default | Notes |
|---|---|---|
| `instance_class` | `db.t3.micro` | Free-tier bracket |
| `multi_az` | `true` | Set `false` in dev to halve cost |
| `engine_version` / `parameter_group_family` | `16` / `postgres16` | Keep in sync |
| `backup_retention_period` | `3` | Days |
| `ssm_password_parameter_name` | `/config/ecommerce-api/spring.datasource.password` | App reads this |

## Outputs

`db_instance_endpoint`, `db_instance_address`, `db_security_group_id`,
`db_password` (**sensitive**), `ssm_password_parameter_name`.
