<div align="center">

# Hardened Headless E-Commerce REST API

**Production-grade headless commerce backend engineered for automated DevSecOps pipelines**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![AWS](https://img.shields.io/badge/AWS-Multi--Tier-FF9900?logo=amazon-aws&logoColor=white)](https://aws.amazon.com/)
[![Terraform](https://img.shields.io/badge/Terraform-IaC-7B42BC?logo=terraform&logoColor=white)](https://www.terraform.io/)
[![OWASP ZAP](https://img.shields.io/badge/OWASP_ZAP-DAST-00549E?logo=owasp&logoColor=white)](https://www.zaproxy.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

---

## Table of Contents

- [Overview](#overview)
- [DevSecOps Pipeline Architecture](#devsecops-pipeline-architecture)
- [Two-Phase Ephemeral Infrastructure](#two-phase-ephemeral-infrastructure)
- [Security Hardening](#security-hardening)
- [Domain Data Model](#domain-data-model)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Local Development](#local-development)

---

## Overview

This project is a **REST API backend for a headless e-commerce platform**, designed as a full DevSecOps reference implementation. Security is treated as a first-class citizen — not a post-deployment afterthought.

Every pull request into `main` triggers a fully automated **5-job security validation pipeline** that runs secret scanning, SAST analysis, infrastructure compliance checks, SCA/container hardening, and active DAST fuzzing against an ephemeral multi-tier AWS environment — all before a single line of code can be merged.

### Tech Stack

| Layer | Technology |
|---|---|
| **Runtime** | Java 17 (Eclipse Temurin), Spring Boot 3.5.x |
| **Database** | AWS RDS PostgreSQL 16, HikariCP connection pool |
| **Infrastructure** | AWS VPC · ALB · EC2 Auto Scaling Group · RDS |
| **IaC** | Terraform (modular, remote S3 backend state) |
| **CI/CD** | GitHub Actions (DAG multi-job pipeline) |
| **Security Tools** | Trufflehog · Semgrep · Checkov · Trivy · OWASP ZAP |
| **Container** | Docker (eclipse-temurin:17-jre-alpine base) |
| **Auth** | AWS OIDC Keyless Federation (no static credentials) |

---

## DevSecOps Pipeline Architecture

Pull requests trigger a fully parallelized **Directed Acyclic Graph (DAG)** workflow in GitHub Actions. Uncoupled static checks run in parallel; rigid compliance gates block cloud provisioning until all upstream jobs pass.

```
[ git push → pull_request on main ]
               │
               ▼
 ┌─────────────────────────────────┐
 │  JOB 1 — Static Security Scans │  ← Runs parallel to nothing; blocks all downstream
 │                                 │
 │  ● Trufflehog  (Secret Scan)    │
 │  ● Semgrep     (SAST / OWASP)   │
 │  ● Checkov     (IaC Compliance) │
 └───────────────┬─────────────────┘
                 │ needs: static-security-scans
                 ▼
 ┌─────────────────────────────────┐
 │  JOB 2 — Build & Verify         │
 │                                 │
 │  ● Maven compile + package      │
 │  ● Trivy FS   (SCA scan)        │
 │  ● Docker build                 │
 │  ● Trivy Image (container scan) │
 └───────────────┬─────────────────┘
                 │ needs: build-and-verify
                 ▼
 ┌─────────────────────────────────┐
 │  JOB 3 — Ephemeral AWS Deploy   │
 │                                 │
 │  ● Two-phase S3 staging         │
 │  ● VPC · ALB · ASG · RDS map   │
 └───────────────┬─────────────────┘
                 │ needs: ephemeral-deploy
                 ▼
 ┌─────────────────────────────────┐
 │  JOB 4 — Active DAST            │
 │                                 │
 │  ● ALB health-check polling     │
 │  ● OWASP ZAP baseline scan      │
 └──────────┬─────────────┬────────┘
            │ if: always()│ needs: ephemeral-deploy + dast
            ▼             ▼
 ┌───────────────┐  ┌─────────────────────────────┐
 │  JOB 5        │  │  JOB 6 — Branch Gate        │
 │  Teardown     │  │                             │
 │  (guaranteed) │  │  ● Atomic compliance status │
 └───────────────┘  └─────────────────────────────┘
```

### Security Gates in Detail

#### Job 1 — Static Security Scans

| Tool | Purpose | Failure Behavior |
|---|---|---|
| **Trufflehog** | Scans full git commit history for verified secrets (API keys, tokens, passwords) | Hard fail — blocks pipeline |
| **Semgrep** | SAST against `p/java` + `p/owasp-top-ten` rulesets | Hard fail — blocks pipeline |
| **Checkov** | Terraform IaC compliance (IMDSv2, S3 encryption, security group lockdown) | Hard fail with curated `skip_check` profile for ephemeral sandbox constraints |

#### Job 2 — Compilation & Supply Chain Vetting

| Tool | Purpose | Failure Behavior |
|---|---|---|
| **Maven** | Compiles with pinned `tomcat.version: 10.1.55` and `postgresql.version: 42.7.11` to block transitive RCE vulnerabilities | Hard fail |
| **Trivy FS** | SCA scan of all file system dependencies — zero tolerance for `CRITICAL`/`HIGH` CVEs | Hard fail |
| **Trivy Image** | Container layer analysis on `eclipse-temurin:17-jre-alpine` | Hard fail |

#### Job 6 — Branch Protection Gate

An umbrella job (`secure-validation-gate`) aggregates the outcome of all upstream jobs into a single atomic status check that GitHub's branch protection rules evaluate before allowing a merge to `main`.

---

## Two-Phase Ephemeral Infrastructure

The pipeline solves a non-trivial distribution problem: GitHub Actions runner nodes are ephemeral VMs with no network path into the private AWS subnets where compute lives. The **Two-Phase Staging Bucket Pattern** bridges this gap cleanly.

### Phase 1 — Storage Provisioning

Terraform targets only the S3 deployment bucket (`aws_s3_bucket.app_deploy`):
- Server-side encryption enabled (AES-256)
- Public access blocked
- 24-hour lifecycle expiry rule (ephemeral data hygiene)

The runner then streams three artifacts into the bucket:
- `app.jar` — the pre-vetted, pre-compiled Spring Boot binary
- `Dockerfile` — a staging-optimized image built **from the JAR**, not from source
- `docker-compose.yml` — the container orchestration spec

> **Why a separate Dockerfile?** The development `Dockerfile` rebuilds from `src/` and `pom.xml`, which don't exist on the EC2 host. The staging variant uses `COPY app.jar` directly, keeping the image minimal and reproducible.

### Phase 2 — Compute Bootstrapping

Terraform applies the full network topology:

```
Internet
   │
   ▼
AWS ALB (Public Subnets, HTTP:80)
   │
   ▼
EC2 Auto Scaling Group (Private Subnets)
   │ user_data bootstrap script:
   │  1. Pull app.jar + Dockerfile from S3
   │  2. Resolve RDS endpoint via IAM metadata
   │  3. Write /app/.env with live credentials
   │  4. docker compose up
   │
   ▼
AWS RDS PostgreSQL 16 (Isolated Subnet Group)
```

The `user_data` bootstrap script uses the EC2 instance's IAM profile to fetch runtime credentials from Secrets Manager and write a local `.env` file dynamically — no static credentials in source control, ever.

> **Why decouple the database?** Running PostgreSQL alongside the Java application inside a `t3.micro` (1 GB RAM) instance triggers the Linux OOM killer. Routing connections to managed RDS keeps the compute layer stateless and horizontally scalable.

---

## Security Hardening

All security measures are implemented in code — no manual server configuration required.

### 1. HTTP Security Headers — `SecurityHeadersFilter`

A global `jakarta.servlet.Filter` (priority `@Order(1)`) intercepts every outbound HTTP response — regardless of controller, path, or status code — and injects the following headers:

| Header | Value | Fixes |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | ZAP [10021] — MIME sniffing attacks |
| `Cross-Origin-Resource-Policy` | `same-origin` | ZAP [90004] — Cross-origin resource leakage |
| `Cache-Control` | `no-store` | ZAP [10049] — Sensitive response caching |
| `X-Frame-Options` | `DENY` | Clickjacking / UI redressing |
| `X-XSS-Protection` | `0` | Disables legacy broken browser XSS filter (OWASP recommendation) |

### 2. Information Disclosure Controls — `BaseUtilityController`

Unmapped administrative paths that would otherwise trigger Spring Boot's default 500 error page (leaking framework details) are explicitly intercepted and return sanitized JSON responses:

| Path | Behavior | HTTP Status |
|---|---|---|
| `/actuator/` | `{"error": "Not Found"}` — path existence is not confirmed | **404** |
| `/api/v1/admin/` | `{"error": "Forbidden"}` — structured, non-disclosive | **403** |

### 3. Error Payload Sanitization — `application.properties`

Framework error signatures are stripped from all client-facing responses:

```properties
# Disable Spring Boot white-label error page
server.error.whitelabel.enabled=false

# Strip all framework debug information from error responses
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-stacktrace=never
server.error.include-exception=false
```

### 4. Structured Exception Handling — `GlobalExceptionHandler`

A `@RestControllerAdvice` handler produces consistent, OWASP-compliant error payloads for all exception types without leaking stack traces or internal class names:

```json
{
  "timestamp": "2026-06-23T14:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product with id 42 was not found.",
  "path": "/api/products/42"
}
```

### 5. OWASP ZAP DAST Scan Results

Active DAST scan results after hardening (production target):

```
PASS: Loosely Scoped Cookie          [90033]
PASS: X-Content-Type-Options         [10021]  ← Fixed by SecurityHeadersFilter
PASS: Information Disclosure         [10023]  ← Fixed by explicit endpoint handlers
PASS: Cross-Origin-Resource-Policy   [90004]  ← Fixed by SecurityHeadersFilter
PASS: Application Error Disclosure   [90022]  ← Fixed by explicit endpoint handlers
IGNORE: Non-Storable Content         [10049]  ← HTTP 500/204 are non-cacheable by spec

FAIL-NEW: 0  WARN-NEW: 0  PASS: 62+
```

---

## Domain Data Model

```
┌──────────┐      1:1       ┌──────────┐      1:N      ┌────────────┐
│   User   ├───────────────►│   Cart   ├──────────────►│  CartItem  │
│          │                │          │                │            │
│ id       │                │ id       │                │ id         │
│ username │                │ user_id  │                │ cart_id    │
│ email    │                └──────────┘                │ product_id │
│createdAt │                                            │ quantity   │
└──────────┘                                            └─────┬──────┘
     │                                                        │
     │ 1:N                                                    │ N:1
     ▼                                                        ▼
┌───────────┐      1:N      ┌───────────┐      N:1     ┌─────────────┐
│   Order   ├──────────────►│ OrderItem ├─────────────►│   Product   │
│           │               │           │               │             │
│ id        │               │ id        │               │ id          │
│ user_id   │               │ order_id  │               │ name        │
│ status    │               │ product_id│               │ description │
│totalAmount│               │ quantity  │               │ price       │
│ createdAt │               │priceAtPurchase            │ category    │
└───────────┘               └───────────┘               │stockQuantity│
                                                        │ createdAt   │
                                                        └─────────────┘
```

**Design decisions:**

- `CartItem` enforces a unique constraint on `(cart_id, product_id)` — duplicate `POST` calls increment quantity rather than creating duplicate rows.
- `OrderItem.priceAtPurchase` snapshots the price at checkout time — product price changes never retroactively affect historical order data.
- `Order.@PrePersist` null-guards `createdAt` to allow the `DataSeeder` to inject realistic historical timestamps for demo data.
- All lazy-loaded associations use `JOIN FETCH` in repository queries to eliminate N+1 query patterns.

---

## API Reference

All endpoints return `application/json`. Endpoints that operate on cart and order data require an `X-User-Id` header.

### Users — `/api/users`

<details>
<summary><code>GET /api/users/{id}</code> — Retrieve a user profile</summary>

```bash
curl http://localhost:8080/api/users/1
```

```json
{
  "id": 1,
  "username": "alice_dev",
  "email": "alice.devlin@techcorp.io",
  "createdAt": "2026-06-16T17:00:00"
}
```
</details>

<details>
<summary><code>POST /api/users</code> — Create a new user account</summary>

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{ "username": "newuser", "email": "new@example.com" }'
```
</details>

---

### Products — `/api/products`

<details>
<summary><code>GET /api/products</code> — List all products with optional filters</summary>

Supports `?category=` (exact match) and `?search=` (case-insensitive substring).

```bash
curl "http://localhost:8080/api/products?category=Fitness&search=yoga"
```
</details>

<details>
<summary><code>POST /api/products</code> — Create a new product</summary>

Validates: price ≥ 0.01, non-blank name, stock quantity ≥ 0.

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{ "name": "Yoga Mat Pro", "price": 49.99, "category": "Fitness", "stockQuantity": 100 }'
```
</details>

---

### Cart — `/api/carts`

Requires `X-User-Id: {userId}` header on all requests.

<details>
<summary><code>GET /api/carts</code> — Retrieve the active cart with computed totals</summary>

```bash
curl http://localhost:8080/api/carts -H "X-User-Id: 1"
```
</details>

<details>
<summary><code>POST /api/carts/items</code> — Add an item to the cart</summary>

Validates stock availability in real time. Duplicate product adds increment quantity.

```bash
curl -X POST http://localhost:8080/api/carts/items \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{ "productId": 3, "quantity": 2 }'
```
</details>

---

### Orders — `/api/orders`

<details>
<summary><code>POST /api/orders/checkout</code> — Atomic checkout transaction</summary>

Executes inside a single `@Transactional` boundary:
1. Validates stock levels for every cart item
2. Decrements inventory counters atomically
3. Snapshots `priceAtPurchase` for each line item
4. Creates the `Order` record with status `PENDING`
5. Clears the user's cart

```bash
curl -X POST http://localhost:8080/api/orders/checkout \
  -H "X-User-Id: 1"
```
</details>

---

## Project Structure

```
devsecops_project02/
│
├── .github/workflows/
│   └── devsecops-pipeline.yml       # 6-job parallel DAG CI/CD pipeline
│
├── .zap/
│   └── rules.tsv                    # OWASP ZAP custom rule overrides
│
├── terraform/infra-network/         # Full AWS multi-tier IaC footprint
│   ├── vpc.tf                       # VPC, subnets, IGW, NAT Gateway
│   ├── alb.tf                       # Application Load Balancer
│   ├── asg.tf                       # Auto Scaling Group + Launch Template
│   ├── rds.tf                       # RDS PostgreSQL 16 cluster
│   └── providers.tf                 # AWS provider + S3 backend config
│
├── Dockerfile                       # Multi-stage container build (local)
├── docker-compose.yml               # Local development stack
├── pom.xml                          # Dependency management with version pins
│
└── src/main/
    ├── java/com/ecommerce/api/
    │   ├── config/
    │   │   ├── SecurityHeadersFilter.java   # Global HTTP security header injector
    │   │   └── DataSeeder.java              # Idempotent demo data seeder
    │   │
    │   ├── controller/
    │   │   ├── BaseUtilityController.java   # Root, robots.txt, sitemap, safe fallbacks
    │   │   ├── UserController.java
    │   │   ├── ProductController.java
    │   │   ├── CartController.java
    │   │   └── OrderController.java
    │   │
    │   ├── exception/
    │   │   └── GlobalExceptionHandler.java  # Unified OWASP-compliant error responses
    │   │
    │   ├── model/                           # JPA entities (User, Product, Cart, Order…)
    │   ├── repository/                      # Spring Data JPA repositories
    │   └── service/                         # Business logic layer
    │
    └── resources/
        └── application.properties           # Hardened production configuration
```

---

## Local Development

### Prerequisites

| Requirement | Minimum Version |
|---|---|
| JDK | 17 (Eclipse Temurin recommended) |
| Maven | 3.9+ |
| Docker Engine | 24+ with Compose Plugin |

### Start the Full Stack

```bash
# Start PostgreSQL 16 + Spring Boot API in isolated containers
docker compose up -d --build

# Stream live application logs
docker compose logs -f api

# Stop and remove containers
docker compose down
```

### Run Tests & Build

```bash
# Run the full test suite
mvn test

# Compile and package the JAR (skip tests)
mvn clean package -DskipTests=true
```

---

<div align="center">

Built with security-first principles · Designed for automated validation pipelines

</div>
