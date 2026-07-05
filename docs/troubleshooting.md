# Troubleshooting Guide

> Symptom → likely cause → fix. Ordered roughly by how often each bites. For the
> procedures referenced here (deploy, rollback, teardown) see
> [`runbook.md`](runbook.md).

## Terraform / IaC

### Module not installed
```
Error: Module not installed ... Run "terraform init"
```
**Cause:** you added/changed a `module` block, or you're in a fresh checkout.
**Fix:** `terraform init` in the environment directory. Re-run after any change to a
module `source`.

### Missing required provider
```
Error: Missing required provider ... hashicorp/aws
```
**Cause:** providers not installed (fresh clone, or `-backend=false` init skipped).
**Fix:** `terraform init` (add `-backend=false` for offline `validate` only).

### Terraform state lock
```
Error acquiring the state lock ... ConditionalCheckFailed / lock file exists
```
**Cause:** a previous run died mid-apply; the native S3 lock file is still held.
**Fix:** confirm no apply is actually running, then
`terraform force-unlock <LOCK_ID>`. Never force-unlock while a real apply is live.

### `fmt -check` fails in CI
**Cause:** unformatted `.tf`.
**Fix:** `terraform fmt -recursive` from `terraform/`, commit the result.

### Backend key confusion (staging vs ephemeral)
Staging's `providers.tf` declares `key = "staging/terraform.tfstate"`, but CI runs
`init -reconfigure -backend-config="key=ephemeral/terraform.tfstate"`. If you deploy
locally and later can't see CI's resources (or vice-versa), you're on a **different
state key**. Re-init with the key you intend to operate on.

### Dependency cycle after editing modules
```
Error: Cycle: module.compute ... module.iam ...
```
**Cause:** you made `iam` reference a `compute` resource output (or vice-versa).
**Fix:** keep the name-based contract — pass `artifact_bucket_name` (a string) to
both; let `iam` rebuild the ARN. See
[terraform-module-usage.md](terraform-module-usage.md#breaking-the-iam--compute-cycle).

### S3 `BucketAlreadyExists` on apply
**Cause:** bucket names are global; a name collided (e.g. deploying `dev` with the
staging `project_name`, or two people sharing an account).
**Fix:** ensure `project_name` is unique per environment. `dev` already uses
`devsecops-p02-dev`; new environments need their own prefix.

## Deploy / runtime

### Unhealthy targets / 502/503
The ALB returns 502/503 and the target group shows instances unhealthy.
**Causes & fixes, in order:**
1. **Instances still bootstrapping.** `user_data` installs Docker, pulls the JAR and
   runs compose — allow ~3–5 min. Watch `/var/log/user-data.log` via SSM.
2. **Artifacts missing in the bucket.** Phase 1 must upload `app.jar`, `Dockerfile`,
   `docker-compose.yml` before Phase 2 apply. Verify: `aws s3 ls s3://$BUCKET/`.
3. **Health check path.** Target group checks `/actuator/health` (HTTP 200). If the
   app changed the actuator path, update `health_check_path` on the compute module.
4. **App crashed.** SSM onto the instance → `docker compose logs`.

### Database connection failures
App logs show connection refused / timeout to Postgres.
1. **Security group:** the DB SG only allows `5432` from the app SG. If the app tier
   SG changed, the wiring in `module.database` (`app_security_group_id`) must point
   at it.
2. **Wrong credentials:** the app reads the password from SSM
   (`/config/ecommerce-api/spring.datasource.password`) / the injected `.env`. After
   a secret rotation, instances must be refreshed to re-read it (runbook §5).
3. **Endpoint drift:** `user_data` writes `RDS_ENDPOINT` from
   `module.database.db_instance_address`. A DB replacement changes the address —
   refresh instances so the new `.env` is written.

### Instance can't download artifacts from S3
`aws s3 cp` fails in `user_data`.
**Cause:** the instance profile lacks read on the bucket, or the bucket name the IAM
policy was scoped to ≠ the bucket that was created.
**Fix:** both `iam` and `compute` must receive the **same** `artifact_bucket_name`
local. Confirm the instance profile is attached (launch template → IAM instance
profile) and the role policy resource ARN matches the bucket.

### OOM / instance thrashing
`t2.micro` has 1 GB RAM. Running Postgres on the instance triggers the OOM killer —
that's why the DB is managed RDS. Don't co-locate a database container on the app
instance; scale the ASG or instance type instead.

## Pipeline gate failures

### Checkov (IaC compliance)
Points at `terraform/environments/staging` and resolves local modules.
- **New finding on a module resource:** fix it in the module (preferred), or, if it's
  an accepted trade-off for the ephemeral sandbox, add the check ID to `skip_check`
  in [the workflow](../.github/workflows/devsecops-pipeline.yml) with a comment.
- Reproduce locally: `checkov -d terraform/environments/staging --framework terraform`.

**Known cross-module false positives.** Checkov's graph checks do not resolve
edges that cross module boundaries via input variables. Two are permanently
skipped with justification:

| Check | Claims | Reality |
|---|---|---|
| `CKV2_AWS_5` (SG not attached) | `network`'s ALB/app SGs are unattached | They're attached in `compute` (`aws_lb.security_groups`, launch template) via `var.*_security_group_id` |
| `CKV2_AWS_11` (no VPC flow logs) | `network`'s VPC has no flow log | `observability` creates one, wired via `var.vpc_id` |

If you split more cross-referencing resources into separate modules, expect the
same class of finding — verify the wiring is real, then skip with a comment.

### Semgrep (SAST)
Fails on `p/java` or `p/owasp-top-ten`. Fix the flagged code; only add to
`.semgrepignore` for a justified false positive.

### Trivy (SCA / container)
Zero tolerance for HIGH/CRITICAL. Bump the dependency (pin in `pom.xml` — see the
existing `tomcat.version` / `postgresql.version` pins) or the base image.

### Trufflehog (secrets)
A verified secret is in git history. Rotate it immediately (assume compromised),
then purge from history. Never "fix" by deleting the line in a new commit alone.

### OWASP ZAP (DAST)
New alert on the live stack. Most are handled by `SecurityHeadersFilter` /
`GlobalExceptionHandler` / `BaseUtilityController`; add genuine false positives to
[`.zap/rules.tsv`](../.zap/rules.tsv) with justification.

### `secure-validation-gate` red but sub-jobs green
The aggregation gate needs **all** upstream jobs to have succeeded. A skipped or
cancelled upstream job fails the gate. Re-run the pipeline; check for a job that was
cancelled (e.g. timeout) rather than failed.

## Getting more signal

| Need | Where |
|---|---|
| Instance boot log | `/var/log/user-data.log` (via SSM Session Manager) |
| App logs | `docker compose logs` on the instance |
| Rejected network traffic | flow-log bucket `devsecops-p02-vpc-flow-logs-99` |
| DB slow queries | RDS logs (`log_min_duration_statement = 1000ms`) |
| Terraform detail | `TF_LOG=DEBUG terraform apply` |
