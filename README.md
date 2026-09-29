# Bazaar

An e-commerce platform built as Spring Boot microservices. Each service owns its database. Services will share common infrastructure components (Redis, RabbitMQ, Keycloak, etc.) as they are added.

## Tech stack

- Java 25, Spring Boot 4.1, Maven (multi-module)
- PostgreSQL 18 with Flyway migrations
- Docker Compose for local infrastructure and apps
- Cloud Native Buildpacks (`spring-boot:build-image`) for OCI images
- Testcontainers for tests and dev-time infrastructure

## Services

| Service             | Port | Database                                | Description                                      |
|---------------------|------|-----------------------------------------|--------------------------------------------------|
| `config-server`     | 8888 | —                                       | Centralized configuration (Spring Cloud Config)  |
| `catalog-service`   | 8081 | `catalog-service-db` (host port 5432)   | Products and categories                          |
| `inventory-service` | 8082 | `inventory-service-db` (host port 5433) | Stock levels and reservations                    |

## Project structure

```
bazaar/
├── pom.xml              # Parent POM (modules, Spring Cloud BOM, shared OCI image config)
├── config-server/       # Spring Cloud Config server
├── config-repo/         # Configuration files served by config-server
├── catalog-service/     # Catalog microservice
├── inventory-service/   # Inventory microservice
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
2. Run `ConfigServerApplication` from the IDE.
3. Run each service's main class from the IDE (e.g. `CatalogServiceApplication`).

Services fetch their settings from the config server at `localhost:8888`, and those settings default to the `localhost` ports published by `infra.yml`, so no extra configuration is needed.

### Option 2: A single service with Testcontainers

Use this to work on one service in isolation without starting anything by hand.

Run the service's test main class from the IDE (e.g. `TestCatalogServiceApplication` in `catalog-service/src/test/java`). It starts the service's own dependencies with Testcontainers (see `ContainersConfig`) and wires the connection details automatically. Docker must be running. No config server is needed: the test classpath disables the config client and imports the service's files from `config-repo/` directly (see `src/test/resources/config/application.yaml`).

Or from the terminal:
```sh
./mvnw -pl catalog-service spring-boot:test-run
```

Containers are annotated with `@RestartScope`, so they survive Spring Boot DevTools restarts. The database is discarded when the application stops.

### Option 3: Everything in Docker

Use this to run the whole system as it would run when deployed.

1. Build the service images (Cloud Native Buildpacks, needs Docker):
   ```sh
   ./mvnw -pl config-server,catalog-service,inventory-service spring-boot:build-image -DskipTests
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

Service settings live in `config-repo/` and are served by `config-server` (see below). Each service's own `application.yaml` only holds its name and how to reach the config server.

The files in `config-repo/<service>.yaml` read connection settings from environment variables, falling back to local defaults that match the ports published by `infra.yml`. The placeholders are resolved by the service, not the config server, so the variables are set on the service:

| Variable      | `catalog-service` default                  | `inventory-service` default                  |
|---------------|--------------------------------------------|----------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/catalog` | `jdbc:postgresql://localhost:5433/inventory` |
| `DB_USERNAME` | `catalog`                                  | `inventory`                                  |
| `DB_PASSWORD` | `catalog`                                  | `inventory`                                  |

`CONFIG_SERVER_URL` (default `http://localhost:8888`) points each service at the config server. Services fail fast if the config server can't be reached; in Docker, `restart: on-failure` restarts them until it is up.

In Docker, `docker/apps.yml` sets them from `docker/.env`. In stage or production, set them to point at managed databases instead of containers.

### Config server

`config-server` runs Spring Cloud Config with the `native` backend and serves the files in `config-repo/`: `application.yaml` for settings shared by every service, and `<spring.application.name>.yaml` for one service. The location comes from `CONFIG_SEARCH_LOCATIONS`, which defaults to `file:../config-repo/` (relative to the `config-server` module, the working directory when run from the IDE or with `./mvnw -pl config-server spring-boot:run`). In Docker, `apps.yml` mounts `config-repo/` read-only at `/config-repo/`.

## Tests

```sh
./mvnw verify
```

Integration tests use Testcontainers, so Docker must be running.
