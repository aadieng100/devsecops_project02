# Operations Runbook

> Operational procedures for **DevSecOps-Project02**: deploy, promote, roll back,
> tear down, rotate secrets, and handle on-call. Pair with
> [`troubleshooting.md`](troubleshooting.md) for symptom-driven fixes.

## System at a glance

```
Internet → ALB (public subnets, :80)
              → Auto Scaling Group (private subnets, Spring Boot :8080)
                  → RDS PostgreSQL 16 (isolated subnets, :5432)
State: S3 (devsecops-p02-remote-state-storage-99) + native S3 locking, KMS-encrypted
Auth:  GitHub OIDC → devsecops-p02-github-actions-role (no static keys)
```

| Environment | State key | Multi-AZ | Deployed by |
|---|---|---|---|
| `staging` | `staging/` (CI overrides to `ephemeral/`) | yes | CI pipeline on PR |
| `dev` | `dev/` | no | Developers, on demand |
| `bootstrap` | `bootstrap/` | n/a | One-time, manual |

## 0. One-time backend bootstrap

Only run if the state bucket / OIDC role do not yet exist. Uses **local** state.

```bash
cd terraform/bootstrap
terraform init
terraform apply          # creates state S3 bucket, KMS key, GitHub OIDC role
terraform output         # note github_actions_role_arn + s3_bucket_name
```

The `state_bucket` has `prevent_destroy = true` — deleting it is a deliberate,
multi-step operation, never routine.

## 1. Deploy staging (what CI does — mirror it locally to debug)

The pipeline uses the **two-phase staging bucket pattern** because runners have no
network path into the private subnets:

```bash
cd terraform/environments/staging
terraform init -reconfigure -backend-config="key=ephemeral/terraform.tfstate"
terraform validate

# Phase 1 — create ONLY the artifact bucket
terraform apply -target=module.compute.aws_s3_bucket.app_deploy \
  -auto-approve -var="db_username=postgres"

# Stage artifacts
BUCKET=$(terraform output -raw app_deploy_bucket_name)
# Paths are relative to terraform/environments/staging (3 levels below repo root)
aws s3 cp ../../../target/headless-ecommerce-api-1.0.0.jar s3://$BUCKET/app.jar
aws s3 cp <staging Dockerfile>                              s3://$BUCKET/Dockerfile
aws s3 cp ../../../docker-compose.yml                       s3://$BUCKET/docker-compose.yml

# Phase 2 — apply the full stack; instances pull artifacts on boot
terraform apply -auto-approve -var="db_username=postgres"

# Grab the entrypoint
echo "http://$(terraform output -raw alb_dns_name)"
```

### Verify a deploy

```bash
ALB=$(terraform output -raw alb_dns_name)
curl -fsS "http://$ALB/actuator/health"     # expect {"status":"UP"}
```

Give the ASG instances a few minutes: `user_data` installs Docker, pulls the JAR,
writes `/app/.env`, and runs `docker compose up` before the target turns healthy.

## 2. Deploy / refresh dev

```bash
cd terraform/environments/dev
terraform init
terraform apply
```

## 3. Roll back

The stack is immutable and artifact-driven — "rollback" = re-point at the previous
known-good artifact and refresh instances.

```bash
# Re-stage the previous good JAR under the same key
aws s3 cp app-PREVIOUS.jar s3://$BUCKET/app.jar
# Force the ASG to replace instances (they re-pull on boot)
aws autoscaling start-instance-refresh \
  --auto-scaling-group-name "$(terraform output -raw autoscaling_group_name 2>/dev/null || echo devsecops-p02-asg)"
```

For an infra rollback, `git revert` the offending Terraform change and re-run the
pipeline (or `terraform apply` locally). Because state is remote and versioned, the
previous configuration re-converges cleanly.

## 4. Tear down

```bash
# staging (ephemeral copy CI uses)
cd terraform/environments/staging
terraform init -reconfigure -backend-config="key=ephemeral/terraform.tfstate"
terraform destroy -auto-approve -var="db_username=postgres"

# dev
cd terraform/environments/dev && terraform destroy
```

`force_destroy = true` on the artifact and flow-log buckets and
`skip_final_snapshot = true` on RDS make teardown clean and cost-free. **Never** run
destroy against `bootstrap`.

## 5. Rotate the database secret

The password is a `random_password` mirrored to SSM
(`/config/ecommerce-api/spring.datasource.password`).

```bash
cd terraform/environments/staging
terraform apply -replace="module.database.random_password.db_password" \
  -var="db_username=postgres"
# RDS master password + SSM SecureString update together; refresh instances so the
# app re-reads the new credential.
```

## 6. On-call

### Triage order

1. **Is it up?** `curl http://$ALB/actuator/health`.
2. **Are targets healthy?** EC2 → Target Groups → `devsecops-p02-app-tg`.
3. **Did the last deploy pass?** Check the PR's pipeline run + `secure-validation-gate`.
4. **App logs:** SSM Session Manager onto an instance → `docker compose logs` and
   `/var/log/user-data.log`.
5. **Network:** VPC flow logs bucket (`devsecops-p02-vpc-flow-logs-99`) for REJECTs.

### Common incidents → jump to

| Symptom | See |
|---|---|
| Targets unhealthy / 502 from ALB | [troubleshooting](troubleshooting.md#unhealthy-targets--502503) |
| DB connection refused | [troubleshooting](troubleshooting.md#database-connection-failures) |
| Pipeline red on Checkov/Semgrep/Trivy/ZAP | [troubleshooting](troubleshooting.md#pipeline-gate-failures) |
| Terraform state lock stuck | [troubleshooting](troubleshooting.md#terraform-state-lock) |

### Escalation

Include: environment, ALB URL, failing job/run URL, last `terraform apply` output,
and `/var/log/user-data.log` from an affected instance. Roll back (§3) first if a
recent deploy is the likely cause; investigate second.

## Routine maintenance

| Cadence | Task |
|---|---|
| Per PR | Pipeline runs all security gates automatically |
| Weekly | Review Trivy/Semgrep findings trend; bump pinned dep versions in `pom.xml` |
| Monthly | Review Checkov `skip_check` list — retire any no-longer-needed skips |
| Quarterly | Rotate DB secret (§5); review IAM policies for least privilege |
