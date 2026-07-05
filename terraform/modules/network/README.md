# Module: `network`

Foundational, multi-tier VPC topology. This is the base layer every other module
plugs into.

## What it provisions

- VPC (`enable_dns_hostnames`, `enable_dns_support`) + drop-all default security group
- Internet Gateway + single cost-optimized NAT Gateway (EIP-backed)
- Three subnet tiers, one subnet per AZ:
  - **public** — ALB / ingress (`map_public_ip_on_launch = true`)
  - **private** — application compute (egress via NAT only)
  - **database** — isolated, local routes only, no internet path
- Route tables + associations for each tier
- Two edge security groups: `alb` (80 from world) and `app` (`app_port` from ALB only)

## Usage

```hcl
module "network" {
  source = "../../modules/network"

  project_name          = "devsecops-p02"
  vpc_cidr              = "10.0.0.0/16"
  public_subnet_cidrs   = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnet_cidrs  = ["10.0.11.0/24", "10.0.12.0/24"]
  database_subnet_cidrs = ["10.0.21.0/24", "10.0.22.0/24"]
  availability_zones    = ["eu-west-3a", "eu-west-3b"]
  app_port             = 8080
}
```

## Inputs

| Name | Type | Default | Description |
|---|---|---|---|
| `project_name` | string | — | Prefix applied to all resource names |
| `vpc_cidr` | string | `10.0.0.0/16` | VPC CIDR block |
| `public_subnet_cidrs` | list(string) | 2× /24 | Public subnet CIDRs (one per AZ) |
| `private_subnet_cidrs` | list(string) | 2× /24 | Private subnet CIDRs (one per AZ) |
| `database_subnet_cidrs` | list(string) | 2× /24 | Isolated DB subnet CIDRs (one per AZ) |
| `availability_zones` | list(string) | 2× eu-west-3 | Target AZs |
| `app_port` | number | `8080` | Application listen port |

> The three CIDR lists and `availability_zones` **must** be the same length.

## Outputs

`vpc_id`, `public_subnet_ids`, `private_subnet_ids`, `database_subnet_ids`,
`alb_security_group_id`, `app_security_group_id`.
