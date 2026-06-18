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
