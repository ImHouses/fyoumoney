# Money Project v2 — Server

A Ktor-based backend server for a personal finance application.

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.3.0 |
| Server framework | Ktor (Netty engine) | 3.4.1 |
| ORM | Jetbrains Exposed | 0.61.0 |
| Database | PostgreSQL | driver 42.7.10 |
| In-memory DB (dev/test) | H2 | 2.3.232 |
| DI | Koin | 4.1.2-Beta1 |
| Serialization | kotlinx-serialization + Gson (future) | — |
| Logging | Logback | 1.4.14 |
| Build | Gradle (Kotlin DSL) | 9.3.0 |
| JVM target | Java 21 | — |

## Project Structure

```
server/
├── build.gradle.kts          # Dependencies and plugins
├── gradle.properties         # Version catalog
├── settings.gradle.kts       # Project name
└── src/
    ├── main/
    │   ├── kotlin/
    │   │   ├── Application.kt      # Entry point, module wiring
    │   │   ├── Databases.kt        # Exposed DB connection (reads from application.yaml)
    │   │   ├── Frameworks.kt       # Koin DI setup
    │   │   ├── Routing.kt          # Route definitions
    │   │   └── Serialization.kt    # ContentNegotiation (kotlinx-json)
    │   └── resources/
    │       ├── application.yaml    # Ktor config (port, DB URL/credentials)
    │       └── logback.xml         # Logging config
    └── test/
        └── kotlin/
            └── ApplicationTest.kt
```

## Configuration

Database connection is configured in `src/main/resources/application.yaml`:

```yaml
postgres:
  url: "jdbc:postgresql://localhost/default"
  user: username
  password: password
```

**Do not commit real credentials.** Use a local override file (e.g., `application.local.yaml`, already gitignored) or environment variables for real deployments.

## Running the Server

```bash
./gradlew run
```

Server starts on port `8080` by default.

## API Documentation

Swagger UI is available at [http://localhost:8080/swagger](http://localhost:8080/swagger) when the server is running in development mode (`ktor.development: true` in `application.yaml`).

The OpenAPI spec is defined in `src/main/resources/openapi/documentation.yaml`. Swagger UI is disabled in production.

## Running Tests

```bash
./gradlew test
```

## Frontend UI Conventions

- **List pages** (TransactionList, EditPlan, SetupPage): `max-width: 600px`. Each list item is a separate card (`background-color: var(--color-bg-card)`, `border-radius: var(--radius-md)`) with `gap: 6px` between them. Do not group items in a single bordered container with dividers.
- **Form pages** (TransactionForm): `max-width: 480px`.

## Package

`dev.jcasas`