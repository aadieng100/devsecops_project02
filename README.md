# 🛒 Headless E-Commerce REST API Engine

> Production-grade headless e-commerce backend built with **Spring Boot 3.3.4 · Java 17 · PostgreSQL**.  
> Designed from the ground up to support downstream **DevSecOps pipelines** — DAST scanning, API fuzzing, penetration testing, and automated security regression.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture & Technology Stack](#2-architecture--technology-stack)
3. [Project Structure](#3-project-structure)
4. [Domain Data Model](#4-domain-data-model)
5. [API Reference](#5-api-reference)
   - [Users](#a-users--apiusers)
   - [Products](#b-products--apiproducts)
   - [Cart](#c-cart--apicarts)
   - [Orders](#d-orders--apiorders)
6. [Error Handling](#6-error-handling)
7. [Data Seeding Framework](#7-data-seeding-framework)
8. [Local Development](#8-local-development)
9. [AWS Deployment Guide (DevSecOps)](#9-aws-deployment-guide-devsecops)
   - [Step 1 — Prerequisites & IAM](#step-1--prerequisites--iam-setup)
   - [Step 2 — ECR: Container Registry](#step-2--ecr-container-registry)
   - [Step 3 — RDS: Managed PostgreSQL](#step-3--rds-managed-postgresql)
   - [Step 4 — Secrets Manager](#step-4--secrets-manager)
   - [Step 5 — VPC & Networking](#step-5--vpc--networking)
   - [Step 6 — ECS Fargate Cluster](#step-6--ecs-fargate-cluster)
   - [Step 7 — ALB & Target Groups](#step-7--application-load-balancer--target-groups)
   - [Step 8 — ECS Task Definition](#step-8--ecs-task-definition)
   - [Step 9 — ECS Service](#step-9--ecs-service)
   - [Step 10 — CI/CD with GitHub Actions](#step-10--cicd-pipeline-github-actions)
   - [Step 11 — Security Hardening](#step-11--security-hardening)
   - [Step 12 — Observability](#step-12--observability--monitoring)
   - [Step 13 — DAST & API Security Scanning](#step-13--dast--api-security-scanning)
10. [Environment Variables Reference](#10-environment-variables-reference)
11. [Health & Readiness Checks](#11-health--readiness-checks)

---

## 1. Project Overview

This service is a **fully headless REST API** — it produces pure JSON and has no server-side rendering. It can be consumed by any frontend (React, mobile, CLI) or automated tooling.

| Attribute | Value |
|---|---|
| Framework | Spring Boot 3.3.4 |
| Java Version | 17 (LTS) |
| Build Tool | Maven 3.x |
| Database | PostgreSQL 16 |
| Connection Pool | HikariCP (20 max connections) |
| Container Base | `eclipse-temurin:17-jre-alpine` |
| Exposed Port | `8080` |
| Health Endpoint | `GET /actuator/health` |

---

## 2. Architecture & Technology Stack

```
┌────────────────────────────────────────────────────────────┐
│                        REST Clients                         │
│          (Browser / Mobile / DAST Scanner / curl)           │
└──────────────────────────┬─────────────────────────────────┘
                           │ HTTP JSON
┌──────────────────────────▼─────────────────────────────────┐
│                  Spring Boot REST Layer                      │
│   UserController  ProductController  CartController         │
│                          OrderController                     │
│   @Valid DTO Validation · X-User-Id Header Context          │
├────────────────────────────────────────────────────────────┤
│                    Service Layer                             │
│  UserServiceImpl  ProductServiceImpl  CartServiceImpl       │
│                   OrderServiceImpl                           │
│  @Transactional boundaries · Business rule enforcement      │
├────────────────────────────────────────────────────────────┤
│                  Repository Layer                            │
│  Spring Data JPA · JPQL JOIN FETCH queries (N+1 safe)      │
├────────────────────────────────────────────────────────────┤
│             PostgreSQL 16 (HikariCP pool)                   │
└────────────────────────────────────────────────────────────┘
```

**Key Libraries:**

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API, Jackson JSON serialisation |
| `spring-boot-starter-data-jpa` | ORM, repository abstraction |
| `spring-boot-starter-validation` | Jakarta Bean Validation (`@Valid`, `@NotBlank`, etc.) |
| `spring-boot-starter-actuator` | `/actuator/health`, `/actuator/metrics` |
| `postgresql` | JDBC driver |
| `lombok` | Boilerplate reduction (`@Builder`, `@Slf4j`, `@RequiredArgsConstructor`) |
| `h2` (test scope) | In-memory DB for context smoke tests |

---

## 3. Project Structure

```
devsecops_project02/
├── Dockerfile                                  # Multi-stage build, non-root runtime user
├── docker-compose.yml                          # Local dev: PostgreSQL 16 + API
├── pom.xml                                     # Spring Boot 3.3.4 Maven project
└── src/
    ├── main/
    │   ├── java/com/ecommerce/api/
    │   │   ├── EcommerceApplication.java       # Entry point
    │   │   │
    │   │   ├── model/                          # JPA Entities & Enums
    │   │   │   ├── User.java
    │   │   │   ├── Product.java
    │   │   │   ├── Cart.java
    │   │   │   ├── CartItem.java
    │   │   │   ├── Order.java
    │   │   │   ├── OrderItem.java
    │   │   │   └── OrderStatus.java            # Enum: PENDING | PAID | SHIPPED
    │   │   │
    │   │   ├── repository/                     # Spring Data JPA Interfaces
    │   │   │   ├── UserRepository.java
    │   │   │   ├── ProductRepository.java      # JPQL: category filter + keyword search
    │   │   │   ├── CartRepository.java         # JOIN FETCH cart + items + products
    │   │   │   ├── CartItemRepository.java
    │   │   │   └── OrderRepository.java        # JOIN FETCH orders + items + products
    │   │   │
    │   │   ├── service/                        # Service interfaces
    │   │   │   ├── UserService.java
    │   │   │   ├── ProductService.java
    │   │   │   ├── CartService.java
    │   │   │   ├── OrderService.java
    │   │   │   └── impl/                       # Implementations
    │   │   │       ├── UserServiceImpl.java
    │   │   │       ├── ProductServiceImpl.java
    │   │   │       ├── CartServiceImpl.java    # Lazy cart creation, stock guard
    │   │   │       └── OrderServiceImpl.java   # 2-phase atomic checkout
    │   │   │
    │   │   ├── controller/                     # REST Controllers (14 endpoints)
    │   │   │   ├── UserController.java
    │   │   │   ├── ProductController.java
    │   │   │   ├── CartController.java
    │   │   │   ├── OrderController.java
    │   │   │   └── dto/                        # Request/Response DTOs
    │   │   │       ├── ProductRequest.java
    │   │   │       ├── AddToCartRequest.java
    │   │   │       ├── UserRequest.java
    │   │   │       ├── CartResponse.java       # Computed subtotals + cart total
    │   │   │       └── UpdateOrderStatusRequest.java
    │   │   │
    │   │   ├── exception/                      # Global error handling
    │   │   │   ├── GlobalExceptionHandler.java # @RestControllerAdvice
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   ├── InsufficientStockException.java
    │   │   │   └── BadRequestException.java
    │   │   │
    │   │   └── config/
    │   │       ├── DataSeeder.java             # 56 products, 5 users, 5 orders on boot
    │   │       └── WebMvcConfig.java           # CORS configuration
    │   │
    │   └── resources/
    │       └── application.properties          # Env-var driven, HikariCP tuned
    │
    └── test/
        └── java/com/ecommerce/api/
            └── EcommerceApplicationTests.java  # H2 context smoke test
```

---

## 4. Domain Data Model

```
┌──────────┐       1:1       ┌──────────┐      1:N     ┌────────────┐
│   User   ├────────────────►│   Cart   ├─────────────►│  CartItem  │
│  id      │                 │  id      │               │  id        │
│  username│                 │  user_id │               │  cart_id   │
│  email   │                 └──────────┘               │  product_id│
│createdAt │                                            │  quantity  │
└──────────┘                                            └─────┬──────┘
     │                                                        │
     │ 1:N                                                    │ N:1
     │                                                        ▼
┌────▼──────┐      1:N     ┌───────────┐     N:1    ┌──────────────┐
│   Order   ├─────────────►│ OrderItem │────────────►│   Product   │
│  id       │              │  id       │              │  id         │
│  user_id  │              │  order_id │              │  name       │
│  status   │              │  product_id              │  description│
│totalAmount│              │  quantity │              │  price      │
│ createdAt │              │priceAtPurch              │  category   │
└───────────┘              └───────────┘              │stockQuantity│
                                                      │  createdAt  │
                                                      └─────────────┘
```

**Entity Design Decisions:**
- `CartItem` has a unique constraint on `(cart_id, product_id)` — duplicate adds increment quantity.
- `OrderItem.priceAtPurchase` snapshots the price at checkout time — price changes don't affect order history.
- `Order.@PrePersist` null-guards `createdAt` so the DataSeeder can inject historical timestamps.
- All lazy-loaded associations use `JOIN FETCH` in repository queries to avoid N+1 queries.

---

## 5. API Reference

All endpoints return `application/json`. Error responses follow the [standard envelope](#6-error-handling).

### A. Users — `/api/users`

#### `GET /api/users/{id}`
Fetch user profile by ID.

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

#### `POST /api/users`
Provision a new user account.

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{ "username": "newuser", "email": "new@example.com" }'
```

**Request Body Constraints:**

| Field | Rules |
|---|---|
| `username` | Not blank, 3–64 chars |
| `email` | Valid RFC email, max 256 chars |

---

### B. Products — `/api/products`

#### `GET /api/products`
List all products. Supports optional filtering:

| Parameter | Type | Behavior |
|---|---|---|
| `category` | string | Exact match (case-sensitive) |
| `search` | string | Case-insensitive substring search on `name` and `description` |

```bash
# All products
curl http://localhost:8080/api/products

# By category
curl "http://localhost:8080/api/products?category=Electronics"

# Keyword search
curl "http://localhost:8080/api/products?search=wireless"

# Combined
curl "http://localhost:8080/api/products?category=Fitness&search=yoga"
```

#### `GET /api/products/{id}`
Fetch single product. Returns `404` if not found.

#### `POST /api/products`
Create a product. All fields are validated.

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "USB-C Hub 7-in-1",
    "description": "Compact aluminium hub with HDMI 4K, 3x USB-A, SD card reader, and 100W PD passthrough.",
    "price": 49.99,
    "category": "Electronics",
    "stockQuantity": 150
  }'
```

**Request Body Constraints:**

| Field | Rules |
|---|---|
| `name` | Not blank, 2–256 chars |
| `description` | Not blank, 10–5000 chars |
| `price` | ≥ 0.01, max 10 integer digits, 2 decimal places |
| `category` | Not blank, max 128 chars |
| `stockQuantity` | ≥ 0 |

#### `PUT /api/products/{id}`
Full replacement update. Same validation as POST.

#### `DELETE /api/products/{id}`
Delete product. Returns `204 No Content`.

---

### C. Cart — `/api/carts`

> All cart endpoints require the `X-User-Id` header identifying the acting user.  
> A cart is created lazily on first access.

#### `GET /api/carts`
Return the active cart with computed subtotals and total.

```bash
curl http://localhost:8080/api/carts -H "X-User-Id: 1"
```

```json
{
  "cartId": 7,
  "userId": 1,
  "items": [
    {
      "cartItemId": 12,
      "productId": 3,
      "productName": "Samsung 65\" Neo QLED 8K Smart TV",
      "unitPrice": 1499.00,
      "quantity": 1,
      "subtotal": 1499.00
    }
  ],
  "cartTotal": 1499.00
}
```

#### `POST /api/carts/items`
Add an item to the cart. If the product already exists in the cart, the quantity is **incremented** (not replaced). Validates against available stock.

```bash
curl -X POST http://localhost:8080/api/carts/items \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{ "productId": 3, "quantity": 1 }'
```

#### `DELETE /api/carts/items/{productId}`
Completely remove a product line from the cart.

```bash
curl -X DELETE http://localhost:8080/api/carts/items/3 -H "X-User-Id: 1"
```

#### `DELETE /api/carts/clear`
Remove all items from the active cart.

```bash
curl -X DELETE http://localhost:8080/api/carts/clear -H "X-User-Id: 1"
```

---

### D. Orders — `/api/orders`

> All order endpoints require the `X-User-Id` header.

#### `POST /api/orders/checkout`
Execute the full atomic checkout transaction.

**Business logic (all within one `@Transactional` boundary):**
1. Fetch the active cart — error if empty.
2. **Validation pass:** verify every product has sufficient stock. Fail fast before any mutation.
3. **Deduction pass:** decrement `Product.stockQuantity` for each line item.
4. Snapshot `priceAtPurchase` for each `OrderItem`.
5. Persist the `Order` with status `PENDING`.
6. Clear the user's cart.

```bash
curl -X POST http://localhost:8080/api/orders/checkout -H "X-User-Id: 1"
```

**Returns:** Full `Order` object with `OrderItem` list, `201 Created`.

#### `GET /api/orders`
Retrieve all historical orders for the user, newest first.

```bash
curl http://localhost:8080/api/orders -H "X-User-Id: 1"
```

#### `PATCH /api/orders/{id}/status`
Update the status of an existing order.

```bash
curl -X PATCH http://localhost:8080/api/orders/2/status \
  -H "Content-Type: application/json" \
  -d '{ "status": "PAID" }'
```

Valid statuses: `PENDING`, `PAID`, `SHIPPED`

---

## 6. Error Handling

Every error response conforms to a canonical JSON envelope:

```json
{
  "timestamp": "2026-06-16T17:15:00.123Z",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient stock for product 'Sony WH-1000XM5' (id=1): requested 200 but only 120 available.",
  "productId": 1,
  "requested": 200,
  "available": 120,
  "path": "/api/carts/items"
}
```

| Exception | HTTP Status | Triggered By |
|---|---|---|
| `ResourceNotFoundException` | `404 Not Found` | Missing user/product/cart/order |
| `InsufficientStockException` | `409 Conflict` | Add-to-cart or checkout over stock |
| `BadRequestException` | `400 Bad Request` | Business rule violation (e.g., empty cart checkout, duplicate username) |
| `MethodArgumentNotValidException` | `400 Bad Request` | Jakarta `@Valid` field failures — returns `fieldErrors` map |
| `MissingRequestHeaderException` | `400 Bad Request` | Missing `X-User-Id` header |
| `MethodArgumentTypeMismatchException` | `400 Bad Request` | Wrong type in path variable |
| `Exception` (fallback) | `500 Internal Server Error` | Unexpected failures |

---

## 7. Data Seeding Framework

The `DataSeeder` `CommandLineRunner` boots the application with realistic test data on every fresh database. It is **fully idempotent** — it checks `userRepository.count() > 0` before doing any work, making container restarts safe.

**Seed Dataset:**

| Category | Count | Price Range |
|---|---|---|
| Electronics | 11 products | $99.99 – $2,499.00 |
| Apparel | 11 products | $24.95 – $349.00 |
| Home & Kitchen | 11 products | $39.90 – $799.99 |
| Books | 12 products | $27.99 – $79.99 |
| Fitness | 11 products | $44.95 – $2,495.00 |
| **Total** | **56 products** | **$24.95 – $2,495.00** |

**Seeded Users:**

| ID | Username | Email |
|---|---|---|
| 1 | `alice_dev` | alice.devlin@techcorp.io |
| 2 | `bob_qatester` | bob.qa@devops-lab.com |
| 3 | `carol_infosec` | carol.sec@securenet.org |
| 4 | `dave_sre` | dave.sre@cloudops.net |
| 5 | `eve_dast` | eve.dast@pentest.tools |

**Historical Orders:**

| # | User | Items | Status |
|---|---|---|---|
| 1 | alice_dev | Sony Headphones + Logitech Mouse | `SHIPPED` |
| 2 | bob_qatester | 3 technical books | `PAID` |
| 3 | carol_infosec | Yoga Mat x2 + Hydro Flask x3 | `SHIPPED` |
| 4 | dave_sre | Instant Pot + Nespresso + Cast Iron Skillet | `PENDING` |
| 5 | eve_dast | Levi's Jeans + Nike AF1 + Anker Power Bank | `PAID` |

---

## 8. Local Development

### Prerequisites

- Java 17 (`/usr/local/Cellar/openjdk@17/...` on macOS)
- Maven 3.9+
- Docker & Docker Compose

> ⚠️ **macOS Note:** If your system Maven defaults to Java 24 (check with `mvn --version`), prefix all Maven commands with the `JAVA_HOME` override shown below.

### Option A — Docker Compose (Recommended)

```bash
# Start PostgreSQL + API
docker compose up -d

# Tail logs
docker compose logs -f api

# Stop all services
docker compose down

# Full teardown including volume (resets DB)
docker compose down -v
```

API is available at: `http://localhost:8080`

### Option B — Local PostgreSQL + Maven

```bash
export JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home

# Create database (first time)
createdb -U postgres ecommerce_db

# Run the application
JAVA_HOME=$JAVA_HOME mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="\
    -DDB_URL=jdbc:postgresql://localhost:5432/ecommerce_db \
    -DDB_USERNAME=postgres \
    -DDB_PASSWORD=yourpassword"
```

### Building the JAR

```bash
JAVA_HOME=$JAVA_HOME mvn -B clean package -DskipTests
java -jar target/headless-ecommerce-api-1.0.0.jar
```

### Running Tests

```bash
# Smoke test (H2 in-memory, no PostgreSQL required)
JAVA_HOME=$JAVA_HOME mvn test
```

---

## 9. AWS Deployment Guide (DevSecOps)

This section is a full, step-by-step production deployment guide targeting **AWS ECS Fargate** backed by **RDS PostgreSQL**, with a **GitHub Actions CI/CD** pipeline, full **VPC isolation**, and **DAST scanning** integration.

```
┌────────────────────────────────────────────────────────────────────────┐
│                         AWS Account                                     │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                         VPC (10.0.0.0/16)                         │  │
│  │                                                                   │  │
│  │  ┌─────────────────┐      ┌──────────────────────────────────┐   │  │
│  │  │  Public Subnets  │      │         Private Subnets           │   │  │
│  │  │  (2 AZs)         │      │         (2 AZs)                   │   │  │
│  │  │                 │      │                                   │   │  │
│  │  │  ┌──────────┐  │      │  ┌─────────────────┐             │   │  │
│  │  │  │   ALB    │  │      │  │  ECS Fargate    │             │   │  │
│  │  │  │  :443    │──┼──────┼─►│  Tasks (API)    │             │   │  │
│  │  │  └──────────┘  │      │  │  :8080          │             │   │  │
│  │  │                 │      │  └────────┬────────┘             │   │  │
│  │  └─────────────────┘      │           │                       │   │  │
│  │                           │  ┌────────▼────────┐             │   │  │
│  │                           │  │  RDS PostgreSQL │             │   │  │
│  │                           │  │  (Multi-AZ)     │             │   │  │
│  │                           │  └─────────────────┘             │   │  │
│  │                           └──────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ECR (Container Registry)    Secrets Manager    CloudWatch Logs         │
└────────────────────────────────────────────────────────────────────────┘
```

---

### Step 1 — Prerequisites & IAM Setup

**Install required tools:**

```bash
# AWS CLI v2
brew install awscli
aws configure  # Enter: Access Key, Secret Key, Region (e.g. us-east-1), output format (json)

# Verify
aws sts get-caller-identity
```

**Create a dedicated IAM role for ECS task execution:**

```bash
# Create the task execution role
aws iam create-role \
  --role-name ecommerce-ecs-task-execution-role \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": { "Service": "ecs-tasks.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }]
  }'

# Attach the AWS-managed execution policy (allows ECR pull + CloudWatch logging)
aws iam attach-role-policy \
  --role-name ecommerce-ecs-task-execution-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

# Add Secrets Manager read permission (for DB credentials)
aws iam attach-role-policy \
  --role-name ecommerce-ecs-task-execution-role \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
```

> **Security Note:** In production, scope the Secrets Manager policy to only the specific secret ARN using a custom inline policy, not the broad `SecretsManagerReadWrite`.

---

### Step 2 — ECR: Container Registry

```bash
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export AWS_REGION=us-east-1
export ECR_REPO=ecommerce-api

# Create the private ECR repository
aws ecr create-repository \
  --repository-name $ECR_REPO \
  --image-scanning-configuration scanOnPush=true \
  --encryption-configuration encryptionType=AES256 \
  --region $AWS_REGION

# Enable tag immutability (prevent overwriting tags — important for deployments)
aws ecr put-image-tag-mutability \
  --repository-name $ECR_REPO \
  --image-tag-mutability IMMUTABLE \
  --region $AWS_REGION

# Authenticate Docker to ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS \
  --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Build and push the image (from project root)
export IMAGE_TAG=$(git rev-parse --short HEAD)
export FULL_IMAGE=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG

docker build -t $FULL_IMAGE .
docker push $FULL_IMAGE

echo "Image pushed: $FULL_IMAGE"
```

> **Security:** Enable ECR image scanning (`scanOnPush=true`) to automatically run vulnerability scans (powered by Amazon Inspector) on every pushed image.

---

### Step 3 — RDS: Managed PostgreSQL

```bash
# Create a DB subnet group spanning two private subnets
aws rds create-db-subnet-group \
  --db-subnet-group-name ecommerce-db-subnet-group \
  --db-subnet-group-description "Ecommerce DB subnets" \
  --subnet-ids subnet-AAAAAAAA subnet-BBBBBBBB

# Create a security group for RDS (only allow traffic from ECS security group)
aws ec2 create-security-group \
  --group-name ecommerce-rds-sg \
  --description "RDS PostgreSQL SG" \
  --vpc-id vpc-XXXXXXXXXX

# Allow PostgreSQL port only from the ECS task security group
aws ec2 authorize-security-group-ingress \
  --group-id sg-RDS_SG_ID \
  --protocol tcp \
  --port 5432 \
  --source-group sg-ECS_TASK_SG_ID

# Provision Multi-AZ PostgreSQL 16 instance
aws rds create-db-instance \
  --db-instance-identifier ecommerce-postgres \
  --db-instance-class db.t3.medium \
  --engine postgres \
  --engine-version 16.3 \
  --master-username ecommerce_admin \
  --master-user-password "$(openssl rand -base64 32)" \
  --db-name ecommerce_db \
  --allocated-storage 20 \
  --storage-type gp3 \
  --storage-encrypted \
  --multi-az \
  --vpc-security-group-ids sg-RDS_SG_ID \
  --db-subnet-group-name ecommerce-db-subnet-group \
  --backup-retention-period 7 \
  --deletion-protection \
  --no-publicly-accessible \
  --tags Key=Project,Value=ecommerce Key=Environment,Value=production

# Wait for instance to become available (takes ~5 minutes)
aws rds wait db-instance-available --db-instance-identifier ecommerce-postgres

# Get the endpoint
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-postgres \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

---

### Step 4 — Secrets Manager

Store all sensitive configuration in AWS Secrets Manager so credentials are never baked into container images or task definitions.

```bash
# Store database credentials as a JSON secret
aws secretsmanager create-secret \
  --name /ecommerce/production/db \
  --description "Ecommerce API database credentials" \
  --secret-string '{
    "DB_URL": "jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/ecommerce_db",
    "DB_USERNAME": "ecommerce_admin",
    "DB_PASSWORD": "YOUR_GENERATED_PASSWORD"
  }' \
  --region $AWS_REGION

# Note the Secret ARN for use in the Task Definition
aws secretsmanager describe-secret \
  --secret-id /ecommerce/production/db \
  --query 'ARN' --output text
```

---

### Step 5 — VPC & Networking

> If you are using an existing VPC, skip creation. Ensure you have at least 2 public subnets (for the ALB) and 2 private subnets (for ECS tasks and RDS).

```bash
# Create VPC
VPC_ID=$(aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --query 'Vpc.VpcId' --output text)
aws ec2 modify-vpc-attribute --vpc-id $VPC_ID --enable-dns-hostnames

# Public subnets (ALB)
PUB_SUBNET_A=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID --cidr-block 10.0.1.0/24 \
  --availability-zone ${AWS_REGION}a --query 'Subnet.SubnetId' --output text)

PUB_SUBNET_B=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID --cidr-block 10.0.2.0/24 \
  --availability-zone ${AWS_REGION}b --query 'Subnet.SubnetId' --output text)

# Private subnets (ECS + RDS)
PRIV_SUBNET_A=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID --cidr-block 10.0.10.0/24 \
  --availability-zone ${AWS_REGION}a --query 'Subnet.SubnetId' --output text)

PRIV_SUBNET_B=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID --cidr-block 10.0.11.0/24 \
  --availability-zone ${AWS_REGION}b --query 'Subnet.SubnetId' --output text)

# Internet Gateway
IGW_ID=$(aws ec2 create-internet-gateway --query 'InternetGateway.InternetGatewayId' --output text)
aws ec2 attach-internet-gateway --internet-gateway-id $IGW_ID --vpc-id $VPC_ID

# NAT Gateway (for private subnets to pull images from ECR)
EIP_ID=$(aws ec2 allocate-address --domain vpc --query 'AllocationId' --output text)
NAT_ID=$(aws ec2 create-nat-gateway \
  --subnet-id $PUB_SUBNET_A --allocation-id $EIP_ID \
  --query 'NatGateway.NatGatewayId' --output text)
aws ec2 wait nat-gateway-available --nat-gateway-ids $NAT_ID

# Route tables (public → IGW, private → NAT)
PUB_RT=$(aws ec2 create-route-table --vpc-id $VPC_ID --query 'RouteTable.RouteTableId' --output text)
aws ec2 create-route --route-table-id $PUB_RT --destination-cidr-block 0.0.0.0/0 --gateway-id $IGW_ID
aws ec2 associate-route-table --route-table-id $PUB_RT --subnet-id $PUB_SUBNET_A
aws ec2 associate-route-table --route-table-id $PUB_RT --subnet-id $PUB_SUBNET_B

PRIV_RT=$(aws ec2 create-route-table --vpc-id $VPC_ID --query 'RouteTable.RouteTableId' --output text)
aws ec2 create-route --route-table-id $PRIV_RT --destination-cidr-block 0.0.0.0/0 --nat-gateway-id $NAT_ID
aws ec2 associate-route-table --route-table-id $PRIV_RT --subnet-id $PRIV_SUBNET_A
aws ec2 associate-route-table --route-table-id $PRIV_RT --subnet-id $PRIV_SUBNET_B
```

**Security Groups:**

```bash
# ALB Security Group (public traffic)
ALB_SG=$(aws ec2 create-security-group \
  --group-name ecommerce-alb-sg --description "ALB SG" \
  --vpc-id $VPC_ID --query 'GroupId' --output text)
aws ec2 authorize-security-group-ingress --group-id $ALB_SG --protocol tcp --port 443 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-id $ALB_SG --protocol tcp --port 80  --cidr 0.0.0.0/0

# ECS Task Security Group (only from ALB)
ECS_SG=$(aws ec2 create-security-group \
  --group-name ecommerce-ecs-sg --description "ECS Task SG" \
  --vpc-id $VPC_ID --query 'GroupId' --output text)
aws ec2 authorize-security-group-ingress \
  --group-id $ECS_SG --protocol tcp --port 8080 --source-group $ALB_SG
```

---

### Step 6 — ECS Fargate Cluster

```bash
# Create the cluster
aws ecs create-cluster \
  --cluster-name ecommerce-cluster \
  --settings name=containerInsights,value=enabled \
  --tags key=Project,value=ecommerce

# Create CloudWatch Log Group for API logs
aws logs create-log-group --log-group-name /ecs/ecommerce-api --region $AWS_REGION
aws logs put-retention-policy --log-group-name /ecs/ecommerce-api --retention-in-days 30
```

---

### Step 7 — Application Load Balancer & Target Groups

```bash
# Create ALB in public subnets
ALB_ARN=$(aws elbv2 create-load-balancer \
  --name ecommerce-alb \
  --subnets $PUB_SUBNET_A $PUB_SUBNET_B \
  --security-groups $ALB_SG \
  --scheme internet-facing \
  --type application \
  --query 'LoadBalancers[0].LoadBalancerArn' --output text)

# Create Target Group (port 8080, HTTP health check at /actuator/health)
TG_ARN=$(aws elbv2 create-target-group \
  --name ecommerce-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-protocol HTTP \
  --health-check-path /actuator/health \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 10 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3 \
  --query 'TargetGroups[0].TargetGroupArn' --output text)

# Create HTTPS listener (requires ACM certificate)
# First, request a certificate in ACM for your domain:
# aws acm request-certificate --domain-name api.yourdomain.com --validation-method DNS

aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:us-east-1:ACCOUNT:certificate/YOUR_CERT_ID \
  --default-actions Type=forward,TargetGroupArn=$TG_ARN

# HTTP → HTTPS redirect
aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTP --port 80 \
  --default-actions \
    Type=redirect,RedirectConfig='{Protocol=HTTPS,Port=443,StatusCode=HTTP_301}'
```

---

### Step 8 — ECS Task Definition

Save the following as `task-definition.json` and register it:

```json
{
  "family": "ecommerce-api",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecommerce-ecs-task-execution-role",
  "taskRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecommerce-ecs-task-execution-role",
  "containerDefinitions": [
    {
      "name": "ecommerce-api",
      "image": "ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/ecommerce-api:IMAGE_TAG",
      "portMappings": [
        { "containerPort": 8080, "protocol": "tcp" }
      ],
      "secrets": [
        {
          "name": "DB_URL",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:/ecommerce/production/db:DB_URL::"
        },
        {
          "name": "DB_USERNAME",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:/ecommerce/production/db:DB_USERNAME::"
        },
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:/ecommerce/production/db:DB_PASSWORD::"
        }
      ],
      "environment": [
        { "name": "SERVER_PORT", "value": "8080" },
        { "name": "SPRING_PROFILES_ACTIVE", "value": "production" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/ecommerce-api",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1"
        ],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 60
      },
      "readonlyRootFilesystem": true,
      "linuxParameters": {
        "initProcessEnabled": true
      }
    }
  ]
}
```

```bash
# Register the task definition
aws ecs register-task-definition \
  --cli-input-json file://task-definition.json
```

---

### Step 9 — ECS Service

```bash
aws ecs create-service \
  --cluster ecommerce-cluster \
  --service-name ecommerce-api-service \
  --task-definition ecommerce-api \
  --launch-type FARGATE \
  --desired-count 2 \
  --network-configuration "awsvpcConfiguration={
    subnets=[$PRIV_SUBNET_A,$PRIV_SUBNET_B],
    securityGroups=[$ECS_SG],
    assignPublicIp=DISABLED
  }" \
  --load-balancers "targetGroupArn=$TG_ARN,containerName=ecommerce-api,containerPort=8080" \
  --deployment-configuration "minimumHealthyPercent=100,maximumPercent=200" \
  --deployment-controller type=ECS \
  --enable-execute-command

# Wait for the service to stabilise
aws ecs wait services-stable \
  --cluster ecommerce-cluster \
  --services ecommerce-api-service

echo "Service deployed!"
```

**Auto Scaling (optional but recommended):**

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/ecommerce-cluster/ecommerce-api-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# Scale on CPU utilisation > 70%
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/ecommerce-cluster/ecommerce-api-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name ecommerce-cpu-scaling \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleOutCooldown": 60,
    "ScaleInCooldown": 120
  }'
```

---

### Step 10 — CI/CD Pipeline (GitHub Actions)

Create `.github/workflows/deploy.yml` in your repository:

```yaml
name: Build, Scan & Deploy to AWS ECS

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY: ecommerce-api
  ECS_CLUSTER: ecommerce-cluster
  ECS_SERVICE: ecommerce-api-service
  CONTAINER_NAME: ecommerce-api
  JAVA_VERSION: '17'

jobs:
  # ──────────────────────────────────────────
  # Job 1: Build, Test, Static Analysis
  # ──────────────────────────────────────────
  build-and-test:
    name: Build & Test
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin
          cache: maven

      - name: Run unit tests
        run: mvn -B test

      - name: Build JAR (skip tests — already ran)
        run: mvn -B package -DskipTests

      - name: Upload JAR artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: target/*.jar

  # ──────────────────────────────────────────
  # Job 2: SAST — Static Application Security Testing
  # ──────────────────────────────────────────
  sast:
    name: SAST Scan (Semgrep)
    runs-on: ubuntu-latest
    needs: build-and-test
    steps:
      - uses: actions/checkout@v4
      - name: Run Semgrep
        uses: semgrep/semgrep-action@v1
        with:
          config: >-
            p/java
            p/owasp-top-ten
            p/spring
        env:
          SEMGREP_APP_TOKEN: ${{ secrets.SEMGREP_APP_TOKEN }}

  # ──────────────────────────────────────────
  # Job 3: Container Build & ECR Push
  # ──────────────────────────────────────────
  push-to-ecr:
    name: Build & Push Container
    runs-on: ubuntu-latest
    needs: [build-and-test, sast]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    outputs:
      image-tag: ${{ steps.build-push.outputs.image-tag }}
      full-image: ${{ steps.build-push.outputs.full-image }}
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build, tag & push image
        id: build-push
        env:
          REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        run: |
          IMAGE_TAG="${GITHUB_SHA::8}"
          FULL_IMAGE="$REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG"
          docker build -t "$FULL_IMAGE" .
          docker push "$FULL_IMAGE"
          echo "image-tag=$IMAGE_TAG" >> $GITHUB_OUTPUT
          echo "full-image=$FULL_IMAGE" >> $GITHUB_OUTPUT

      - name: Run Amazon Inspector container scan
        run: |
          aws ecr describe-image-scan-findings \
            --repository-name $ECR_REPOSITORY \
            --image-id imageTag=${{ steps.build-push.outputs.image-tag }} \
            --region $AWS_REGION || true

  # ──────────────────────────────────────────
  # Job 4: Deploy to ECS
  # ──────────────────────────────────────────
  deploy:
    name: Deploy to ECS Fargate
    runs-on: ubuntu-latest
    needs: push-to-ecr
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Download current task definition
        run: |
          aws ecs describe-task-definition \
            --task-definition ecommerce-api \
            --query taskDefinition \
            > task-definition.json

      - name: Update container image in task definition
        id: update-td
        uses: aws-actions/amazon-ecs-render-task-definition@v1
        with:
          task-definition: task-definition.json
          container-name: ${{ env.CONTAINER_NAME }}
          image: ${{ needs.push-to-ecr.outputs.full-image }}

      - name: Deploy to ECS service
        uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: ${{ steps.update-td.outputs.task-definition }}
          service: ${{ env.ECS_SERVICE }}
          cluster: ${{ env.ECS_CLUSTER }}
          wait-for-service-stability: true

  # ──────────────────────────────────────────
  # Job 5: DAST — Dynamic Application Security Testing
  # ──────────────────────────────────────────
  dast:
    name: DAST Scan (OWASP ZAP)
    runs-on: ubuntu-latest
    needs: deploy
    steps:
      - uses: actions/checkout@v4

      - name: Run OWASP ZAP API Scan
        uses: zaproxy/action-api-scan@v0.7.0
        with:
          target: 'https://api.yourdomain.com'
          format: openapi
          cmd_options: '-a'
          fail_action: false   # Set to true to fail the pipeline on HIGH findings

      - name: Upload ZAP Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: zap-report
          path: report_html.html
```

**Required GitHub Secrets:**

| Secret | Description |
|---|---|
| `AWS_ACCESS_KEY_ID` | IAM user access key (use OIDC in production) |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret key |
| `SEMGREP_APP_TOKEN` | Semgrep Cloud token (optional, for dashboard) |

> **Best Practice:** Replace static IAM credentials with **GitHub OIDC** (`aws-actions/configure-aws-credentials` with `role-to-assume`) to eliminate long-lived credentials from your CI.

---

### Step 11 — Security Hardening

#### WAF (Web Application Firewall)

```bash
# Create a WAF WebACL with common managed rules
aws wafv2 create-web-acl \
  --name ecommerce-waf \
  --scope REGIONAL \
  --default-action Allow={} \
  --rules '[
    {
      "Name": "AWSManagedRulesCommonRuleSet",
      "Priority": 1,
      "OverrideAction": {"None": {}},
      "Statement": {
        "ManagedRuleGroupStatement": {
          "VendorName": "AWS",
          "Name": "AWSManagedRulesCommonRuleSet"
        }
      },
      "VisibilityConfig": {
        "SampledRequestsEnabled": true,
        "CloudWatchMetricsEnabled": true,
        "MetricName": "CommonRuleSet"
      }
    },
    {
      "Name": "AWSManagedRulesSQLiRuleSet",
      "Priority": 2,
      "OverrideAction": {"None": {}},
      "Statement": {
        "ManagedRuleGroupStatement": {
          "VendorName": "AWS",
          "Name": "AWSManagedRulesSQLiRuleSet"
        }
      },
      "VisibilityConfig": {
        "SampledRequestsEnabled": true,
        "CloudWatchMetricsEnabled": true,
        "MetricName": "SQLiRuleSet"
      }
    }
  ]' \
  --visibility-config \
    SampledRequestsEnabled=true,CloudWatchMetricsEnabled=true,MetricName=ecommerce-waf \
  --region $AWS_REGION

# Associate WAF with the ALB
aws wafv2 associate-web-acl \
  --web-acl-arn arn:aws:wafv2:us-east-1:ACCOUNT:regional/webacl/ecommerce-waf/ID \
  --resource-arn $ALB_ARN \
  --region $AWS_REGION
```

#### GuardDuty (Threat Detection)

```bash
# Enable GuardDuty in the account/region
aws guardduty create-detector --enable --finding-publishing-frequency FIFTEEN_MINUTES
```

#### Security Hub (Aggregated Findings)

```bash
# Enable Security Hub with AWS Foundational Security Best Practices
aws securityhub enable-security-hub \
  --enable-default-standards \
  --region $AWS_REGION
```

#### RDS Encryption & Audit Logging

Verify RDS has encryption at rest (`--storage-encrypted` was set at creation) and enable PostgreSQL audit logs:

```bash
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-postgres \
  --enable-cloudwatch-logs-exports '["postgresql", "upgrade"]' \
  --apply-immediately
```

#### ECS Task Hardening

The `task-definition.json` already includes:
- `"readonlyRootFilesystem": true` — prevents file system tampering inside the container.
- `"initProcessEnabled": true` — ensures zombie processes are reaped.
- Credentials injected from **Secrets Manager** via `secrets[]`, never environment variables with plaintext.

---

### Step 12 — Observability & Monitoring

#### CloudWatch Dashboard

```bash
aws cloudwatch put-dashboard \
  --dashboard-name EcommerceAPI \
  --dashboard-body '{
    "widgets": [
      {
        "type": "metric",
        "properties": {
          "title": "ALB Request Count",
          "metrics": [["AWS/ApplicationELB","RequestCount","LoadBalancer","ecommerce-alb"]],
          "period": 60, "stat": "Sum", "view": "timeSeries"
        }
      },
      {
        "type": "metric",
        "properties": {
          "title": "ALB 5xx Errors",
          "metrics": [["AWS/ApplicationELB","HTTPCode_ELB_5XX_Count","LoadBalancer","ecommerce-alb"]],
          "period": 60, "stat": "Sum", "view": "timeSeries"
        }
      },
      {
        "type": "metric",
        "properties": {
          "title": "ECS CPU Utilisation",
          "metrics": [["AWS/ECS","CPUUtilization","ClusterName","ecommerce-cluster","ServiceName","ecommerce-api-service"]],
          "period": 60, "stat": "Average", "view": "timeSeries"
        }
      },
      {
        "type": "metric",
        "properties": {
          "title": "RDS Database Connections",
          "metrics": [["AWS/RDS","DatabaseConnections","DBInstanceIdentifier","ecommerce-postgres"]],
          "period": 60, "stat": "Average", "view": "timeSeries"
        }
      }
    ]
  }'
```

#### Critical Alarms

```bash
# 5xx error rate alarm
aws cloudwatch put-metric-alarm \
  --alarm-name ecommerce-5xx-errors \
  --metric-name HTTPCode_Target_5XX_Count \
  --namespace AWS/ApplicationELB \
  --dimensions Name=LoadBalancer,Value=ecommerce-alb \
  --period 60 --evaluation-periods 2 \
  --statistic Sum --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:ACCOUNT:ecommerce-alerts

# ECS task count too low (service degraded)
aws cloudwatch put-metric-alarm \
  --alarm-name ecommerce-task-count-low \
  --metric-name RunningTaskCount \
  --namespace ECS/ContainerInsights \
  --dimensions Name=ClusterName,Value=ecommerce-cluster Name=ServiceName,Value=ecommerce-api-service \
  --period 60 --evaluation-periods 2 \
  --statistic Average --threshold 1 \
  --comparison-operator LessThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:ACCOUNT:ecommerce-alerts
```

#### Viewing Logs

```bash
# Tail live ECS container logs
aws logs tail /ecs/ecommerce-api --follow --region $AWS_REGION

# Filter for ERROR-level logs
aws logs filter-log-events \
  --log-group-name /ecs/ecommerce-api \
  --filter-pattern "ERROR" \
  --region $AWS_REGION
```

---

### Step 13 — DAST & API Security Scanning

The seeded dataset is purpose-built to support automated security tooling on boot.

#### OWASP ZAP API Scan (manual)

```bash
docker run --rm \
  -v $(pwd):/zap/wrk:rw \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-api-scan.py \
  -t https://api.yourdomain.com \
  -f openapi \
  -r zap-report.html \
  -a
```

#### Nuclei (Template-based scanning)

```bash
# Install nuclei
brew install nuclei

# Run against the API with community templates
nuclei -u https://api.yourdomain.com \
  -t ~/nuclei-templates \
  -tags api,owasp \
  -o nuclei-report.txt
```

#### Quick Smoke Test via curl

```bash
BASE=https://api.yourdomain.com

# Health check
curl $BASE/actuator/health

# Retrieve seeded products
curl "$BASE/api/products?category=Electronics"

# Cart workflow for seeded user 1
curl -X POST "$BASE/api/carts/items" \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 1}'

curl "$BASE/api/carts" -H "X-User-Id: 1"

# Checkout
curl -X POST "$BASE/api/orders/checkout" -H "X-User-Id: 1"

# View order history
curl "$BASE/api/orders" -H "X-User-Id: 1"

# Negative test: insufficient stock (expect 409)
curl -X POST "$BASE/api/carts/items" \
  -H "X-User-Id: 2" \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 99999}'
```

---

## 10. Environment Variables Reference

| Variable | Default | Required | Description |
|---|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ecommerce_db` | ✅ Production | Full JDBC connection string |
| `DB_USERNAME` | `ecommerce_user` | ✅ Production | Database username |
| `DB_PASSWORD` | `ecommerce_pass` | ✅ Production | Database password |
| `SERVER_PORT` | `8080` | ❌ | HTTP port to bind |
| `SPRING_PROFILES_ACTIVE` | *(none)* | ❌ | Set to `production` to adjust log levels |

---

## 11. Health & Readiness Checks

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Spring Boot health (DB connectivity, disk space) |
| `GET /actuator/info` | Application version metadata |
| `GET /actuator/metrics` | JVM, HTTP request, and HikariCP pool metrics |

ALB health checks poll `GET /actuator/health` every 30 seconds. Tasks are considered unhealthy after 3 consecutive failures and are automatically replaced by ECS.

---

## License

MIT — see [LICENSE](LICENSE) file.
