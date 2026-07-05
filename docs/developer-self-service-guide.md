# Developer Self-Service Guide

> How to go from a laptop to a running, security-vetted change **without filing a
> ticket**. This is the autonomy layer on top of the [Terraform pattern
> library](terraform-module-usage.md).

## What you can do yourself

| I want to… | Do this | You do **not** need |
|---|---|---|
| Run the app locally | `docker compose up -d --build` | AWS access |
| Stand up my own cloud sandbox | Deploy the `dev` environment | Anyone to provision it for you |
| Ship a change to staging | Open a PR to `main` | Manual approval before scans pass |
| Add a new piece of infra | Compose an existing module, or write one | To touch another team's env |

## 1. Local loop (fastest — start here)

```bash
git clone <repo> && cd devsecops_project02
docker compose up -d --build      # PostgreSQL 16 + Spring Boot API
curl localhost:8080/actuator/health
docker compose logs -f api
docker compose down
```

Build & test the JAR:

```bash
mvn test
mvn clean package -DskipTests=true
```

## 2. Your own cloud sandbox (the `dev` environment)

The `dev` environment is a low-cost, fully namespaced twin of staging. Because
every resource name derives from `project_name = "devsecops-p02-dev"`, it coexists
with staging in the same account — nothing to coordinate.

### Prerequisites

- Terraform `>= 1.5` (CI pins `1.15.6`)
- AWS credentials for the sandbox account (`aws sts get-caller-identity` works)
- The backend already exists (created once by `terraform/bootstrap`)

### Stand it up

```bash
cd terraform/environments/dev
terraform init
terraform plan          # tfvars are auto-loaded; review the plan
terraform apply
```

### Tear it down (do this when you're done — it costs money)

```bash
terraform destroy
```

> `dev` runs a **single-AZ** database and a max-1 ASG on purpose. Do not
> load-test correctness-of-HA here — that's what `staging` is for.

## 3. Ship a change (the paved road to staging)

1. Branch, make the change (app code and/or `terraform/`).
2. Push and open a PR into `main`.
3. The [DevSecOps pipeline](../.github/workflows/devsecops-pipeline.yml) runs
   automatically — you get feedback in the PR, no one has to trigger it:

   | Gate | What it checks |
   |---|---|
   | Trufflehog | No committed secrets |
   | Semgrep | SAST (Java + OWASP Top 10) |
   | Checkov | Terraform compliance (on `environments/staging`) |
   | Trivy | Dependency + container CVEs |
   | Ephemeral deploy + OWASP ZAP | Real AWS stack, live DAST scan |

4. Green `secure-validation-gate` → mergeable. Red → read the failing job log and
   the [troubleshooting guide](troubleshooting.md).

## 4. Changing infrastructure yourself

You almost never write raw resources — you **compose modules**.

- **Tune an existing environment** (size, Multi-AZ, retention): edit the env's
  `terraform.tfvars`. No module changes.
- **Add a capability that already exists as a module**: add a `module` block to the
  environment's `main.tf`, wire inputs/outputs, `terraform init`.
- **Add a genuinely new capability**: write a module under `terraform/modules/`
  following the [module usage guide](terraform-module-usage.md#adding-a-new-module),
  then consume it.

### Guardrails that keep self-service safe

- **No static cloud credentials** anywhere — CI uses GitHub OIDC federation;
  secrets are generated at apply time and stored in SSM.
- **Checkov + Semgrep + Trivy + ZAP** run on every PR, so an unsafe change is
  caught before merge, not after.
- **Ephemeral by default** — the pipeline builds and destroys the staging stack per
  run, so a mistake can't quietly linger.

## 5. When you're stuck

1. [`troubleshooting.md`](troubleshooting.md) — symptom → cause → fix.
2. [`runbook.md`](runbook.md) — operational procedures (deploy, rollback, teardown,
   secret rotation) and on-call steps.
3. Still stuck → escalate per the runbook's on-call section with the failing job URL
   and any `terraform plan`/`apply` output.
