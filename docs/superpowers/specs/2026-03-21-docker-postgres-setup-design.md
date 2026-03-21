# Docker Compose + Postgres Setup

## Context

The Ktor backend expects a PostgreSQL database but none is provisioned. Running the app fails because there's no database to connect to. We need a simple way to spin up Postgres for local development and have the app create its own schema on startup.

## Decision: Infrastructure-only Docker Compose

Docker Compose runs Postgres only. The backend and frontend continue to run locally (`./gradlew run`, `npm run dev`) for fast iteration and debugger access.

## Decision: Exposed auto-create for schema

The app creates tables on startup using `SchemaUtils.create()`. The schema is already defined in Kotlin table objects — no need for a separate SQL file.

## Design

### 1. `docker-compose.yml` (repo root)

Single Postgres 17 service. Named volume for data persistence. Credentials match existing `application.yaml` defaults. Healthcheck ensures Postgres is ready before the backend tries to connect.

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

### 2. Schema auto-creation in `Databases.kt`

After `Database.connect()`, run `SchemaUtils.create()` with all table objects inside a transaction. This is idempotent — Exposed skips tables that already exist. Only runs when `ktor.development` is `true`, consistent with how CORS and Swagger are gated.

All tables must be passed in a single `SchemaUtils.create()` call so Exposed can resolve cross-feature foreign key dependencies.

Tables: `Categories`, `Budgets`, `BudgetItems`, `Transactions`.

### 3. No changes to `application.yaml`

Current config already matches:
- URL: `jdbc:postgresql://localhost/default`
- User: `username`
- Password: `password`

## Files to modify

| File | Change |
|---|---|
| `docker-compose.yml` (new) | Postgres service definition with healthcheck |
| `src/main/kotlin/Databases.kt` | Add dev-mode guarded `SchemaUtils.create()` after connect |

## Verification

1. `docker compose up -d --wait` — Postgres starts, healthcheck passes, port 5432 accessible
2. `./gradlew run` — backend starts, tables auto-created
3. `cd web && npm run dev` — frontend loads budget data without errors

To reset the database: `docker compose down -v` (destroys the pgdata volume), then `docker compose up -d --wait`.