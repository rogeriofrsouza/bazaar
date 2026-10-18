# Bazaar

An e-commerce platform built as Spring Boot microservices. Each service owns its database. Services share common infrastructure components (Keycloak, with Redis, RabbitMQ, etc. to come).

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
| `customer-service`  | 8084 | `customer-service-db` (host port 5436)  | Customer profiles                                                  |

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
├── customer-service/    # Customer microservice
└── docker/
    ├── .env             # Variables used by the compose files
    ├── infra.yml        # Infrastructure (databases, Keycloak, ...)
    ├── apps.yml         # Microservice containers
    └── keycloak/        # Realm imported by Keycloak on startup
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

Services fetch their settings from the config server at `localhost:8888`, and those settings default to the `localhost` ports published by `infra.yml`, so no extra configuration is needed. The config server reads them from `config-repo/` on disk, so edits there take effect immediately.

### Option 2: A single service with Testcontainers

Use this to work on one service in isolation without starting anything by hand.

Run the service's test main class from the IDE (e.g. `TestCatalogServiceApplication` in `catalog-service/src/test/java`). It starts the service's own dependencies with Testcontainers (see `ContainersConfig`) and wires the connection details automatically. Docker must be running. No config or discovery server is needed: the test classpath disables both clients and carries its own copy of the settings the service needs (see `src/test/resources/config/application.yaml`).

Or from the terminal:
```sh
./mvnw -pl catalog-service spring-boot:test-run
```

Containers are annotated with `@RestartScope`, so they survive Spring Boot DevTools restarts. The database is discarded when the application stops.

Keycloak is not one of those containers. Public endpoints work without it, but calls that carry a token need the Keycloak from `infra.yml` running to be validated.

### Option 3: Everything in Docker

Use this to run the whole system as it would run when deployed.

1. Build the service images (Cloud Native Buildpacks, needs Docker):
   ```sh
   ./mvnw -pl config-server,discovery-server,api-gateway,catalog-service,inventory-service,order-service,customer-service spring-boot:build-image -DskipTests
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

The `<service>.yaml` files in `config-repo/` read connection settings from environment variables, falling back to local defaults that match the ports published by `infra.yml`. The placeholders are resolved by the service, not the config server, so the variables are set on the service:

| Variable      | `catalog-service` default                  | `inventory-service` default                  | `order-service` default                   | `customer-service` default                  |
|---------------|--------------------------------------------|----------------------------------------------|-------------------------------------------|---------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/catalog` | `jdbc:postgresql://localhost:5433/inventory` | `jdbc:postgresql://localhost:5434/orders` | `jdbc:postgresql://localhost:5436/customer` |
| `DB_USERNAME` | `catalog`                                  | `inventory`                                  | `orders`                                  | `customer`                                  |
| `DB_PASSWORD` | `catalog`                                  | `inventory`                                  | `orders`                                  | `customer`                                  |

`CONFIG_SERVER_URL` (default `http://localhost:8888`) points each service at the config server. Services fail fast if the config server can't be reached; in Docker, `restart: on-failure` restarts them until it is up.

`DISCOVERY_SERVER_URL` (default `http://localhost:8761/eureka/`) points each service at the Eureka server. Services register on startup and keep retrying if it isn't up yet.

`JWT_ISSUER_URI` (default `http://localhost:9090/realms/bazaar`) and `JWT_JWK_SET_URI` (default `http://localhost:9090/realms/bazaar/protocol/openid-connect/certs`) point each service at Keycloak to validate access tokens (see [Security](#security)).

In Docker, `docker/apps.yml` sets them from `docker/.env`. In stage or production, set them to point at managed databases instead of containers.

All servers run on virtual threads (`spring.threads.virtual.enabled`): the shared `application.yaml` in `config-repo/` enables them for the gateway and the services, and `config-server` and `discovery-server` set them in their own `application.yaml`.

### Config server

`config-server` runs Spring Cloud Config with the `native` (filesystem) backend. It serves the files in `CONFIG_SEARCH_LOCATIONS` (default `file:./config-repo/,file:../config-repo/`, relative to the working directory, so it finds the folder both from the repository root, where IntelliJ runs it, and from the `config-server` module directory, where Maven runs it): `application.yaml` for settings shared by every service, and `<spring.application.name>.yaml` for one service. The files are read on every request, so edits take effect without a restart (services pick them up on their next startup or refresh). In Docker, `apps.yml` mounts `config-repo/` read-only at `/config-repo` and sets `CONFIG_SEARCH_LOCATIONS` from `docker/.env`.

### Discovery server

`discovery-server` runs a standalone Spring Cloud Netflix Eureka server on port 8761. Its settings live in its own `application.yaml`, so it does not need the config server. The Eureka dashboard is at `http://localhost:8761`.

### API gateway

`api-gateway` runs Spring Cloud Gateway Server Web MVC on port 8080 and is the single entry point for clients. Its routes live in `config-repo/api-gateway.yaml` and forward by path to `lb://<service>` URIs, which are resolved through Eureka and load balanced with Spring Cloud LoadBalancer:

| Path                                     | Service             |
|------------------------------------------|---------------------|
| `/api/products/**`, `/api/categories/**` | `catalog-service`   |
| `/api/inventory/**`                      | `inventory-service` |
| `/api/orders/**`                         | `order-service`     |
| `/<service>/v3/api-docs`                 | `<service>`         |

The `/<service>/v3/api-docs` route is defined in code (`ApiDocsRouteConfig`), since a YAML `lb://` URI can't take the service name from the path.

#### API documentation

Each service generates its OpenAPI spec with springdoc and serves it at `/v3/api-docs`. The gateway proxies each spec as `/<service>/v3/api-docs` and hosts one Swagger UI for all of them at `http://localhost:8080/swagger-ui.html`, with a dropdown to pick the service. The specs list the gateway (`GATEWAY_URL`, default `http://localhost:8080`) as their server, so "Try it out" requests go through the gateway.

### Keycloak

`infra.yml` runs Keycloak in development mode (`start-dev`) at `http://localhost:9090`. `KEYCLOAK_PORT` is both the port Keycloak listens on inside the container (`KC_HTTP_PORT`) and the published host port, so the URL has the same port from the host and from other containers (`http://keycloak:9090`). The admin console at that URL signs into the `master` realm with `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` from `docker/.env` (`admin` / `admin`).

Keycloak stores its data in its own PostgreSQL instance, `keycloak-db` (host port 5435, `KEYCLOAK_DB_PORT`; `keycloak` / `keycloak` from `KEYCLOAK_DB_USERNAME` / `KEYCLOAK_DB_PASSWORD`), in the `keycloak-db-data` volume.

On startup Keycloak imports `docker/keycloak/bazaar-realm.json`, which defines the `bazaar` realm with two realm roles:

| Role       | Purpose                                   |
|------------|-------------------------------------------|
| `customer` | Shops the catalog and places orders       |
| `admin`    | Manages the catalog, inventory and orders |

The realm also has one client, `bazaar-web`, for first-party frontends and the gateway's Swagger UI. It is a public client (no secret) that logs users in with the authorization code flow and PKCE (`S256`), and accepts redirects to the gateway (`http://localhost:8080/*`). It also allows the password grant, which is there only to fetch tokens during local development.

Two test users are imported, one per role:

| Username   | Password   | Role       |
|------------|------------|------------|
| `customer` | `customer` | `customer` |
| `admin`    | `admin`    | `admin`    |

These users belong to the `bazaar` realm, so the admin console does not accept them (its `admin` is a different account in the `master` realm). Sign in as one of them at the realm's account console, `http://localhost:9090/realms/bazaar/account`, or request a token as shown in [Security](#security).

The import skips a realm that already exists, so after editing the file reset Keycloak's database to import it again. This leaves the service databases alone:

```sh
docker compose -f docker/infra.yml rm -sf keycloak keycloak-db
docker volume rm bazaar_keycloak-db-data
docker compose -f docker/infra.yml up -d
```

### Security

`catalog-service`, `inventory-service`, `order-service` and `customer-service` are OAuth2 resource servers: they accept a JWT access token issued by the `bazaar` realm, sent as `Authorization: Bearer <token>`. The gateway forwards the header unchanged. Each service declares its rules in its own `SecurityConfig`:

| Request                                               | Access                      |
|-------------------------------------------------------|-----------------------------|
| `GET /api/products/**`, `GET /api/categories/**`      | Public                      |
| `GET /v3/api-docs` (read by the gateway's Swagger UI) | Public                      |
| Everything else, including writes on products         | Valid token (401 otherwise) |

Only the `GET` method is opened on the public paths, so an endpoint added later needs a token unless `SecurityConfig` opens it. Roles are not checked yet.

To get a token for a test user, use the password grant on the `bazaar-web` client and send the `access_token` from the response as the bearer token:

```sh
curl -d client_id=bazaar-web -d grant_type=password -d username=customer -d password=customer \
  http://localhost:9090/realms/bazaar/protocol/openid-connect/token
```

`requests.http` has the same request and stores the token for the protected calls below it.

A service validates a token in two steps, each with its own setting in `config-repo/application.yaml`:

- `JWT_JWK_SET_URI` is where the service downloads Keycloak's signing keys. They are fetched when the first token arrives, so services start without Keycloak.
- `JWT_ISSUER_URI` is compared with the token's `iss` claim and is never called.

They are separate because of Docker. Keycloak sets `iss` from the URL the token was requested on, which is `http://localhost:9090` for a client on the host, while a container reaches Keycloak at `http://keycloak:9090`. So `docker/.env` keeps `KEYCLOAK_ISSUER_URI` on `localhost` and points `KEYCLOAK_JWK_SET_URI` at `keycloak`.

## Tests

```sh
./mvnw verify
```

Integration tests use Testcontainers, so Docker must be running.
