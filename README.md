# Bazaar

An e-commerce platform built as Spring Boot microservices. Each service owns its database. Services will share common infrastructure components (Redis, RabbitMQ, Keycloak, etc.) as they are added.

## Tech stack

- Java 25, Spring Boot 4.1, Maven (multi-module)
- PostgreSQL 18 with Flyway migrations
- Docker Compose for local infrastructure and apps
- Cloud Native Buildpacks (`spring-boot:build-image`) for OCI images
- Testcontainers for tests and dev-time infrastructure

## Services

| Service           | Port | Database                            | Description                        |
|-------------------|------|-------------------------------------|------------------------------------|
| `catalog-service` | 8081 | `catalog-service-db` (host port 5432) | Products and categories |

## Project structure

```
bazaar/
├── pom.xml              # Parent POM (modules, shared OCI image config)
├── catalog-service/     # Catalog microservice
└── docker/
    ├── .env             # Variables used by the compose files
    ├── infra.yml        # Infrastructure (databases, brokers, ...)
    └── apps.yml         # Microservice containers
```

## Prerequisites

- JDK 25 and Maven 3.9 (the repo ships `.sdkmanrc`, so running `sdk env` sets both up with SDKMAN!). The Maven wrapper `./mvnw` also works.
- Docker with Docker Compose v2
- IntelliJ IDEA (or any IDE)

## Running the project

### Option 1: Shared infrastructure in Docker, services from the IDE

Use this to run several services together from the IDE.

1. Start the infrastructure:
   ```sh
   docker compose -f docker/infra.yml up -d
   ```
2. Run each service's main class from the IDE (e.g. `CatalogServiceApplication`).

Each service's `application.yaml` defaults to the `localhost` ports published by `infra.yml`, so no extra configuration is needed.

### Option 2: A single service with Testcontainers

Use this to work on one service in isolation without starting anything by hand.

Run the service's test main class from the IDE (e.g. `TestCatalogServiceApplication` in `catalog-service/src/test/java`). It starts the service's own dependencies with Testcontainers (see `ContainersConfig`) and wires the connection details automatically. Docker must be running.

Or from the terminal:
```sh
./mvnw -pl catalog-service spring-boot:test-run
```

Containers are annotated with `@RestartScope`, so they survive Spring Boot DevTools restarts. The database is discarded when the application stops.

### Option 3: Everything in Docker

Use this to run the whole system as it would run when deployed.

1. Build the service images (Cloud Native Buildpacks, needs Docker):
   ```sh
   ./mvnw -pl catalog-service spring-boot:build-image -DskipTests
   ```
   Each image is tagged `rogeriofrsouza/bazaar-<service>:<version>` and `:latest`.
2. Start infrastructure and apps:
   ```sh
   docker compose -f docker/infra.yml -f docker/apps.yml up -d
   ```

Rebuild the image after code changes; Compose does not build it for you.

### Stopping

```sh
docker compose -f docker/infra.yml -f docker/apps.yml down      # keep data
docker compose -f docker/infra.yml -f docker/apps.yml down -v   # also remove volumes
```

## Development with DevTools in IntelliJ

`spring-boot-devtools` restarts the application when compiled classes change. In IntelliJ, either:

- enable **Settings → Build, Execution, Deployment → Compiler → Build project automatically** and **Settings → Advanced Settings → Allow auto-make to start even if developed application is currently running**, or
- set **On 'Update' action / On frame deactivation** to *Update classes and resources* in the run configuration.

## Configuration

Services read their connection settings from environment variables, falling back to the local defaults:

| Variable      | Default (local)                              |
|---------------|----------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/catalog`   |
| `DB_USERNAME` | `catalog`                                    |
| `DB_PASSWORD` | `catalog`                                    |

In Docker, `docker/apps.yml` sets them from `docker/.env`. In stage or production, set them to point at managed databases instead of containers.

## Tests

```sh
./mvnw verify
```

Integration tests use Testcontainers, so Docker must be running.
