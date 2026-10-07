# Bazaar

An e-commerce platform built as Spring Boot microservices. Each service owns its database. Services will share common infrastructure components (Redis, RabbitMQ, Keycloak, etc.) as they are added.

## Tech stack

- Java 26, Spring Boot 4.1, Maven (multi-module)
- PostgreSQL 18 with Flyway migrations
- Docker Compose for local infrastructure and apps
- Cloud Native Buildpacks (`spring-boot:build-image`) for OCI images
- Testcontainers for tests and dev-time infrastructure

## Services

| Service             | Port | Database                                | Description                                                        |
|---------------------|------|-----------------------------------------|--------------------------------------------------------------------|
| `config-server`     | 8888 | —                                       | Centralized configuration (Spring Cloud Config)                    |
| `discovery-server`  | 8761 | —                                       | Service registry (Spring Cloud Netflix Eureka)                     |
| `api-gateway`       | 8080 | —                                       | Single entry point, routes `/api/**` to services, hosts Swagger UI |
| `catalog-service`   | 8081 | `catalog-service-db` (host port 5432)   | Products and categories                                            |
| `inventory-service` | 8082 | `inventory-service-db` (host port 5433) | Stock levels and reservations                                      |
| `order-service`     | 8083 | `order-service-db` (host port 5434)     | Orders and checkout                                                |

## Project structure

```
bazaar/
├── pom.xml              # Parent POM (modules, Spring Cloud BOM, shared OCI image config)
├── config-server/       # Spring Cloud Config server
├── discovery-server/    # Eureka discovery server
├── api-gateway/         # API gateway (Spring Cloud Gateway)
├── catalog-service/     # Catalog microservice
├── inventory-service/   # Inventory microservice
├── order-service/       # Order microservice
└── docker/
    ├── .env             # Variables used by the compose files
    ├── infra.yml        # Infrastructure (databases, brokers, ...)
    └── apps.yml         # Microservice containers
```

## Prerequisites

- JDK 26 and Maven 3.9 (the repo ships `.sdkmanrc`, so running `sdk env` sets both up with SDKMAN!). The Maven wrapper `./mvnw` also works.
- Docker with Docker Compose v2
- IntelliJ IDEA (or any IDE)

## Running the project

### Option 1: Shared infrastructure in Docker, services from the IDE

Use this to run several services together from the IDE.

1. Start the infrastructure:
   ```sh
   docker compose -f docker/infra.yml up -d
   ```
2. Run `ConfigServerApplication`, `DiscoveryServerApplication` and `ApiGatewayApplication` from the IDE.
3. Run each service's main class from the IDE (e.g. `CatalogServiceApplication`).

Services fetch their settings from the config server at `localhost:8888`, and those settings default to the `localhost` ports published by `infra.yml`, so no extra configuration is needed. The config server reads them from the [bazaar-config](https://github.com/rogeriofrsouza/bazaar-config) repository, so local edits there take effect only after they are pushed.

### Option 2: A single service with Testcontainers

Use this to work on one service in isolation without starting anything by hand.

Run the service's test main class from the IDE (e.g. `TestCatalogServiceApplication` in `catalog-service/src/test/java`). It starts the service's own dependencies with Testcontainers (see `ContainersConfig`) and wires the connection details automatically. Docker must be running. No config or discovery server is needed: the test classpath disables both clients and carries its own copy of the settings the service needs (see `src/test/resources/config/application.yaml`).

Or from the terminal:
```sh
./mvnw -pl catalog-service spring-boot:test-run
```

Containers are annotated with `@RestartScope`, so they survive Spring Boot DevTools restarts. The database is discarded when the application stops.

### Option 3: Everything in Docker

Use this to run the whole system as it would run when deployed.

1. Build the service images (Cloud Native Buildpacks, needs Docker):
   ```sh
   ./mvnw -pl config-server,discovery-server,api-gateway,catalog-service,inventory-service,order-service spring-boot:build-image -DskipTests
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

Service settings live in the [bazaar-config](https://github.com/rogeriofrsouza/bazaar-config) repository and are served by `config-server` (see below). Each service's own `application.yaml` only holds its name and how to reach the config server.

The `<service>.yaml` files in bazaar-config read connection settings from environment variables, falling back to local defaults that match the ports published by `infra.yml`. The placeholders are resolved by the service, not the config server, so the variables are set on the service:

| Variable      | `catalog-service` default                  | `inventory-service` default                  | `order-service` default                   |
|---------------|--------------------------------------------|----------------------------------------------|-------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/catalog` | `jdbc:postgresql://localhost:5433/inventory` | `jdbc:postgresql://localhost:5434/orders` |
| `DB_USERNAME` | `catalog`                                  | `inventory`                                  | `orders`                                  |
| `DB_PASSWORD` | `catalog`                                  | `inventory`                                  | `orders`                                  |

`CONFIG_SERVER_URL` (default `http://localhost:8888`) points each service at the config server. Services fail fast if the config server can't be reached; in Docker, `restart: on-failure` restarts them until it is up.

`DISCOVERY_SERVER_URL` (default `http://localhost:8761/eureka/`) points each service at the Eureka server. Services register on startup and keep retrying if it isn't up yet.

In Docker, `docker/apps.yml` sets them from `docker/.env`. In stage or production, set them to point at managed databases instead of containers.

All servers run on virtual threads (`spring.threads.virtual.enabled`): the shared `application.yaml` in bazaar-config enables them for the gateway and the services, and `config-server` and `discovery-server` set them in their own `application.yaml`.

### Config server

`config-server` runs Spring Cloud Config with the git backend. It clones `CONFIG_GIT_URI` (default `https://github.com/rogeriofrsouza/bazaar-config.git`) at `CONFIG_GIT_LABEL` (default `main`) on startup and serves `application.yaml` for settings shared by every service, and `<spring.application.name>.yaml` for one service. Config changes take effect once they are pushed to bazaar-config. Set `CONFIG_GIT_LABEL` to a branch to try unmerged config.

### Discovery server

`discovery-server` runs a standalone Spring Cloud Netflix Eureka server on port 8761. Its settings live in its own `application.yaml`, so it does not need the config server. The Eureka dashboard is at `http://localhost:8761`.

### API gateway

`api-gateway` runs Spring Cloud Gateway Server Web MVC on port 8080 and is the single entry point for clients. Its routes live in `api-gateway.yaml` in bazaar-config and forward by path to `lb://<service>` URIs, which are resolved through Eureka and load balanced with Spring Cloud LoadBalancer:

| Path                                     | Service             |
|------------------------------------------|---------------------|
| `/api/products/**`, `/api/categories/**` | `catalog-service`   |
| `/api/inventory/**`                      | `inventory-service` |
| `/api/orders/**`                         | `order-service`     |
| `/<service>/v3/api-docs`                 | `<service>`         |

The `/<service>/v3/api-docs` route is defined in code (`ApiDocsRouteConfig`), since a YAML `lb://` URI can't take the service name from the path.

#### API documentation

Each service generates its OpenAPI spec with springdoc and serves it at `/v3/api-docs`. The gateway proxies each spec as `/<service>/v3/api-docs` and hosts one Swagger UI for all of them at `http://localhost:8080/swagger-ui.html`, with a dropdown to pick the service. The specs list the gateway (`GATEWAY_URL`, default `http://localhost:8080`) as their server, so "Try it out" requests go through the gateway.

## Tests

```sh
./mvnw verify
```

Integration tests use Testcontainers, so Docker must be running.
