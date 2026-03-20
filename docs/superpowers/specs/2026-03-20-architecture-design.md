# Architecture Design — fyoumoney Server

**Date:** 2026-03-20
**Status:** Approved

## Overview

fyoumoney is a personal finance backend built with Ktor + Kotlin. Its core purpose is tracking personal income and expenses, with room for more advanced features later. The API serves a web frontend (and potentially a mobile client), protected by JWT authentication for a single user.

## Architectural Style

Feature-based vertical slices. Each feature is a self-contained package owning its full stack: models, schema, repository, service, routes, and Koin module. No shared domain layer at this stage.

**Cross-feature access rule:** if a feature needs data from another feature, it may depend on that feature's *service* (injected via Koin), never on its repository or schema directly. This keeps the boundary at the business logic layer.

## Package Structure

```
src/main/kotlin/
├── Application.kt          # Entry point; delegates wiring to configure* functions
├── Databases.kt            # Exposed DB connection (reads from application.yaml)
├── Frameworks.kt           # Registers all Koin feature modules — add new features here
├── Routing.kt              # Mounts all feature routes — add new routes here
├── Serialization.kt        # ContentNegotiation (kotlinx-json primary; Gson available for future use)
└── features/
    ├── auth/
    │   ├── AuthModels.kt       # User data class
    │   ├── AuthSchema.kt       # Exposed table definition
    │   ├── AuthRepository.kt   # Data access via Exposed
    │   ├── AuthService.kt      # Business logic (credential validation, token generation)
    │   ├── AuthRoutes.kt       # POST /auth/login
    │   └── AuthModule.kt       # Koin bindings
    └── transactions/
        ├── TransactionModels.kt
        ├── TransactionSchema.kt
        ├── TransactionRepository.kt
        ├── TransactionService.kt
        ├── TransactionRoutes.kt
        └── TransactionModule.kt
```

Adding a new feature = new folder with these six files + register its Koin module in `Frameworks.kt` + mount its routes in `Routing.kt`.

## Layer Responsibilities

| Layer | File | Responsibility |
|---|---|---|
| Models | `*Models.kt` | Plain data classes, no framework dependencies |
| Schema | `*Schema.kt` | Exposed table definitions, used only by repository |
| Repository | `*Repository.kt` | Data access via Exposed, returns domain models |
| Service | `*Service.kt` | Business logic, calls repository, no Ktor types |
| Routes | `*Routes.kt` | HTTP handling, calls service injected by Koin |
| DI | `*Module.kt` | Koin bindings for this feature |

### Rules
- Routes know only about their service — no direct DB access
- Services know only about their repository — no Ktor types (`call`, `respond`, etc.)
- Repositories know only about Exposed and the schema — no business logic
- Models are plain data classes — usable at any layer

## Data Flow

```
HTTP Request → Routes → Service → Repository → Exposed → PostgreSQL
```

Koin injects the repository into the service, and the service into the routes.

## Transaction Strategy

Exposed requires all DB operations to run inside a `transaction { }` block.

- **Simple operations** (single table read/write): the repository method opens its own `newSuspendedTransaction`.
- **Multi-step atomic operations** (writing to two or more tables): the service wraps the calls in a single `newSuspendedTransaction` and passes the `Transaction` instance explicitly as a parameter to each repository method involved, keeping the boundary visible and testable.

## Database Schema Management

`SchemaUtils.create()` (Exposed built-in) is used during development to apply the schema. This is adequate for now. Migration tooling (Flyway or Liquibase) will be introduced before any production deployment.

## Authentication

JWT-based, stateless. Works identically for web and mobile clients.

- `POST /auth/login` — validates credentials (password hashed with bcrypt), returns a signed JWT
- All protected routes require `Authorization: Bearer <token>`
- Handled by Ktor's `ktor-server-auth` + `ktor-server-auth-jwt` plugins
- Algorithm: HS256

JWT config block in `application.yaml`:

```yaml
jwt:
  secret: "changeme"
  issuer: "fyoumoney"
  expirySeconds: 86400
```

## Database

**Production:** PostgreSQL, configured via `application.yaml`:

```yaml
postgres:
  url: "jdbc:postgresql://localhost/fyoumoney"
  user: username
  password: password
```

**Development/Testing:** H2 in-memory database (`com.h2database:h2`) for fast, dependency-free tests. Repositories are written against Exposed's dialect-agnostic DSL, so both databases work without code changes.

Real credentials must not be committed — use `application.local.yaml` (gitignored) or environment variables.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.0 |
| Server | Ktor 3.4.1 (Netty) |
| ORM | Jetbrains Exposed 0.61.0 |
| Database | PostgreSQL (prod), H2 (dev/test) |
| DI | Koin 4.2.0 |
| Serialization | kotlinx-serialization JSON (primary), Gson (future use) |
| Logging | Logback 1.5.13 |
| Build | Gradle (Kotlin DSL) 9.3.0 |
| JVM | Java 21 |