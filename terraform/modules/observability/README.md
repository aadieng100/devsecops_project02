# Module: `observability`

Network-layer audit trail: VPC Flow Logs delivered to a locked-down, self-expiring
S3 bucket.

## What it provisions

- Encrypted, private S3 bucket with a short lifecycle expiry (cost hygiene)
- The `delivery.logs.amazonaws.com` bucket policy AWS requires to write logs
- A VPC flow log capturing `ACCEPT` + `REJECT` (`traffic_type = "ALL"` by default)

## Usage

```hcl
module "observability" {
  source = "../../modules/observability"

  project_name          = "devsecops-p02"
  vpc_id                = module.network.vpc_id
  flow_logs_bucket_name = "${var.project_name}-vpc-flow-logs-99"
  log_retention_days    = 3
}
```

## Inputs

| Name | Default | Notes |
|---|---|---|
| `flow_logs_bucket_name` | — | Must be globally unique |
| `log_retention_days` | `3` | Objects auto-expire after N days |
| `traffic_type` | `ALL` | `ACCEPT` / `REJECT` / `ALL` |

## Outputs

`flow_logs_bucket_name`, `flow_log_id`.
