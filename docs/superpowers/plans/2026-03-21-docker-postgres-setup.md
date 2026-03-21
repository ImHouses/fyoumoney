# Docker Compose + Postgres Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a one-command Postgres setup for local development and auto-create the schema on app startup.

**Architecture:** Docker Compose runs a single Postgres 17 service with a healthcheck. The Ktor app creates tables on startup via Exposed's `SchemaUtils.create()`, gated behind `ktor.development`.

**Tech Stack:** Docker Compose, PostgreSQL 17, Jetbrains Exposed `SchemaUtils`

**Spec:** `docs/superpowers/specs/2026-03-21-docker-postgres-setup-design.md`

---

### File Map

| File | Action | Responsibility |
|---|---|---|
| `docker-compose.yml` | Create | Postgres service definition |
| `src/main/kotlin/Databases.kt` | Modify | Add schema auto-creation after DB connect |

---

### Task 1: Create `docker-compose.yml`

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: Create the file**

```yaml
# Local development only — do not use these credentials in production
services:
  db:
    image: postgres:17
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: default
      POSTGRES_USER: username
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-ONLY", "pg_isready", "-U", "username"]
      interval: 2s
      timeout: 3s
      retries: 5

volumes:
  pgdata:
```

- [ ] **Step 2: Start Postgres and verify**

```bash
docker compose up -d --wait
```

Expected: Container starts, healthcheck passes, exits with 0.

- [ ] **Step 3: Verify connectivity**

```bash
docker compose exec db psql -U username -d default -c "SELECT 1"
```

Expected: Returns `1`.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml
git commit -m "infra: add Docker Compose with Postgres for local dev"
```

---

### Task 2: Add schema auto-creation to `Databases.kt`

**Files:**
- Modify: `src/main/kotlin/Databases.kt`

- [ ] **Step 1: Update `Databases.kt`**

Add a dev-mode guarded `SchemaUtils.create()` call after `Database.connect()`. All four tables must be in a single call so Exposed resolves foreign key dependencies.

```kotlin
package dev.jcasas

import dev.jcasas.features.budgets.BudgetItems
import dev.jcasas.features.budgets.Budgets
import dev.jcasas.features.categories.Categories
import dev.jcasas.features.transactions.Transactions
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val url = environment.config.property("postgres.url").getString()
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()
    Database.connect(url = url, user = user, password = password, driver = "org.postgresql.Driver")

    val isDevelopment = environment.config
        .propertyOrNull("ktor.development")?.getString()?.toBoolean() ?: false

    if (isDevelopment) {
        transaction {
            SchemaUtils.create(Categories, Budgets, BudgetItems, Transactions)
        }
    }
}
```

- [ ] **Step 2: Build to verify compilation**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run the app and verify tables are created**

```bash
./gradlew run &
sleep 5
docker compose exec db psql -U username -d default -c "\dt"
kill %1
```

Expected: Output lists `categories`, `budgets`, `budget_items`, `transactions` tables.

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/Databases.kt
git commit -m "feat: auto-create database schema on startup in dev mode"
```

---

### Task 3: End-to-end verification

- [ ] **Step 1: Start full stack**

```bash
docker compose up -d --wait
./gradlew run &
cd web && npm run dev &
```

- [ ] **Step 2: Verify frontend loads without CORS or DB errors**

Open `http://localhost:5173` in a browser. The budget page should load (empty data, no errors in console).

- [ ] **Step 3: Stop services**

```bash
kill %1 %2
docker compose down
```