# AGENTS.md

Instructions for AI coding agents working in this repository. See `README.md` for the project overview and run options.

## Project

Bazaar is an e-commerce platform built as Spring Boot microservices (Java 26, Spring Boot 4.1, Maven multi-module). Each service owns its PostgreSQL database. Infrastructure and apps run with Docker Compose. Images are built with Cloud Native Buildpacks.

```
pom.xml              # Parent POM: modules, Spring Cloud BOM, shared spring-boot-maven-plugin image config
config-server/       # Spring Cloud Config server (git backend on bazaar-config, no database)
discovery-server/    # Eureka server, standalone (own application.yaml, no config client, no database)
api-gateway/         # Spring Cloud Gateway Server Web MVC; routes live in bazaar-config (no database)
catalog-service/     # One module per microservice
docker/.env          # Values for compose variables
docker/infra.yml     # Databases, brokers, etc.
docker/apps.yml      # Microservice containers
```

## Workflow rules

- **Don't verify changes at runtime.** Don't start Docker Compose, run the app, or curl endpoints, and don't leave those steps in plans.
- **Verify the build after changes.** Run `./mvnw verify` once you're done, scoped to the affected modules where that makes sense (`./mvnw -pl <module> -am verify`), and report the result. Testcontainers tests run as part of it, so Docker must be running. Run other Maven goals (`compile`, `package`, `spring-boot:build-image`) only when asked.
- **Commits:** use Conventional Commits with a specific type and scope, e.g. `feat(catalog): ...`, `build(pom): ...`, `ci(docker): ...`, `docs(readme): ...`. Use `chore` only for real housekeeping. Commit only when asked.
- Match the style of the surrounding code; `.editorconfig` sets 4-space indentation, LF line endings and 120 columns.

## Code conventions

- **Package by feature:** `com.rogeriofrsouza.bazaar.<service>.<feature>` (e.g. `catalog.product`, `catalog.category`). Each feature holds its entity, repository, service, controller and DTOs.
- **Controllers:** package-private classes, `@RequestMapping("/api/<resource>")`, constructor injection (no `@Autowired`).
- **Services:** public `@Service` with `@Transactional(readOnly = true)` on reads and `@Transactional` on writes. They return DTOs, not entities.
- **DTOs:** records named `XResponse` / `CreateXRequest`, mapped with a static `XResponse.from(entity)`.
- **Errors:** throw `ResponseStatusException`. RFC 9457 problem details are enabled (`spring.mvc.problemdetails.enabled`).
- **Entities:** a public constructor that validates invariants (e.g. `Assert.hasText`), getters only, static factory methods for creation that generate the id (e.g. `Category.create(...)`). It is the only constructor (Spring Data loads rows through it) and takes every field except those the framework writes, such as `@Version`, which Spring Data sets after construction. Every field the application doesn't change is `final`; fields the framework writes (`@Version`, audit timestamps) stay non-final.
- **Ids:** UUIDv7 generated in the application with the JDK (`UUID.ofEpochMillis(System.currentTimeMillis())`) and stored as `uuid` with no database default. Use `java.util.UUID` end to end: entity fields, service parameters, request and response records, and controller bindings (`@PathVariable UUID id`, `@RequestParam List<UUID> ids`). Spring, Jackson and springdoc handle `UUID` natively, so a malformed id becomes a 400 with no extra config.
- **Persistence is Spring Data JDBC** in every service with a database; there is no JPA. Aggregates reference each other by id (`UUID categoryId`), with no mapped associations. Entities inside an aggregate are a `Set` on the root with `@MappedCollection(idColumn = "<root>_id")` (a `List` or `Map` would also need a key column) and have no back-reference to the root (e.g. `Order.items`). They load with the root's `findById`, and Spring Data deletes and re-inserts them on every save of the root. Load flat rows and assemble trees in the service (e.g. `CategoryService.list()`). Optional filters are `Criteria` (in an `<Entity>Criteria` class, `Criteria.empty()` when absent) run through `JdbcAggregateOperations.findAll(query.with(pageable), type)` and wrapped with `PageableExecutionUtils.getPage(..., () -> count(query, type))` (the `findAll(query, type, pageable)` overload is deprecated for removal). Spring Data JDBC treats an aggregate with a non-null id as existing unless it has a `@Version` property that is still null. Aggregates with a `@Version` (e.g. `Product`) can use `save(...)`; persist new ones without it (e.g. `Category`) through `JdbcAggregateOperations.insert(...)`. Audited aggregates need the `@Version`, otherwise `@CreatedDate` is never set. There is no dirty checking: after mutating a loaded aggregate, call `save(...)` explicitly (e.g. `InventoryItemService.reserve`). Row locks use `@Lock(LockMode.PESSIMISTIC_WRITE)` (`org.springframework.data.relational.repository.Lock`) on a derived query. Categories are reference data seeded by `CategorySeeder` (a `CommandLineRunner` that skips when the table isn't empty), not by a migration. Custom type converters and auditing live in `JdbcConfig`.
- **Null-safety (every service with a database, and every new service):** every package is `@NullMarked` (JSpecify) through its `package-info.java`, so types are non-null by default. Mark nullable fields, parameters and return types with `@Nullable` (`org.jspecify.annotations`, written as a type-use annotation: `private @Nullable String description`). New packages get a `package-info.java`.
- **Schema:** Flyway migrations in `src/main/resources/db/migration` (`V<n>__<description>.sql`). Spring Data JDBC has no schema validation, so services rely on their persistence tests (`<Name>PersistenceTests`) to catch mapping mismatches. Never edit an applied migration.

## Configuration and Docker

- Service settings live in the separate [bazaar-config](https://github.com/rogeriofrsouza/bazaar-config) repository (local clone usually at `../bazaar-config`): `application.yaml` for shared settings, `<service>.yaml` for one service. `config-server` reads it with the git backend (`CONFIG_GIT_URI`, `CONFIG_GIT_LABEL`), so changes take effect only after they are pushed. There is no `native` profile and no volume mount; don't reintroduce them. A service's own `application.yaml` holds only `spring.application.name` and the config-client settings (`optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}`, `fail-fast`).
- Config files read connection settings from env vars with **localhost defaults**, e.g. `url: ${DB_URL:jdbc:postgresql://localhost:5432/catalog}`. The client resolves the placeholders. Running from the IDE needs no profile. Containers and deployed environments override the env vars.
- There is no `spring-boot-docker-compose` and no `local` profile. Don't reintroduce them.
- In compose files, never hardcode values under `environment:`. Write `VAR: ${SOME_VAR}` and add `SOME_VAR` to `docker/.env`. Prefix variables with the service name (`CATALOG_DB_URL`).
- Both compose files use project name `bazaar`. Full stack: `docker compose -f docker/infra.yml -f docker/apps.yml up -d`.
- Images come from the parent POM config: `rogeriofrsouza/bazaar-<artifactId>:<version>` plus `:latest`. `apps.yml` references the untagged name (`latest`).

## Testing

- Tests use Testcontainers with `@ServiceConnection`, never the dev database.
- Each service has, in `src/test/java`:
  - `ContainersConfig`: `@TestConfiguration(proxyBeanMethods = false)` declaring the service's containers as `@Bean @ServiceConnection @RestartScope` beans.
  - `Test<Service>Application`: `SpringApplication.from(<Service>Application::main).with(ContainersConfig.class).run(args)`, for dev-time runs with DevTools.
  - Integration tests: `@SpringBootTest` + `@Import(ContainersConfig.class)`.
- Container images in tests match the ones in `docker/infra.yml` (e.g. `postgres:18-alpine`).
- Each service has `src/test/resources/config/application.yaml` that sets `spring.cloud.config.enabled: false` and `eureka.client.enabled: false`, so tests need no config or discovery server. It holds copies of the bazaar-config settings tests rely on (`server.port`, shared `spring.mvc` settings), but not the datasource, which comes from `@ServiceConnection`. Keep these copies in sync when the shared settings change.
- `config-server`, `discovery-server` and `api-gateway` have no containers, so they only have a plain `@SpringBootTest` context-load test.

## Adding a new microservice

1. Create the module `<name>-service` with the parent `com.rogeriofrsouza:bazaar` and add it to `<modules>` in the root `pom.xml`. Declare `spring-boot-maven-plugin` without extra config; the image settings are inherited. Add `spring-cloud-starter-config` and `spring-cloud-starter-netflix-eureka-client`.
2. Use Spring Data JDBC, not JPA. Add `spring-boot-starter-data-jdbc`, `spring-boot-starter-flyway`, `flyway-database-postgresql`, `jspecify` and `postgresql` (runtime), plus `spring-boot-starter-data-jdbc-test`, `spring-boot-starter-flyway-test`, `spring-boot-testcontainers` and `testcontainers-postgresql` for tests; catalog-service's `pom.xml` is the reference. Follow the rules above: UUIDv7 `java.util.UUID` ids, a `JdbcConfig` with `@EnableJdbcAuditing` when aggregates are audited, and a `@NullMarked` `package-info.java` in every package.
3. Pick the next free app port (`catalog-service` uses 8081). Put `server.port` and the datasource settings in `<name>-service.yaml` in bazaar-config; the local `application.yaml` gets `spring.application.name` and the config-client settings, copied from an existing service.
4. Add `<name>-service-db` to `docker/infra.yml` with its own named volume, healthcheck and a **distinct host port** (`<NAME>_DB_PORT` in `.env`), and use the same port in the localhost default in bazaar-config's `<name>-service.yaml`.
5. Add the app to `docker/apps.yml` with `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` coming from `<NAME>_DB_*` in `docker/.env` plus `CONFIG_SERVER_URL: ${CONFIG_SERVER_URL}` and `DISCOVERY_SERVER_URL: ${DISCOVERY_SERVER_URL}`. It `depends_on` its database with `condition: service_healthy` and on `config-server` and `discovery-server` with `condition: service_started`. Set `restart: on-failure` so it restarts until `config-server` is up.
6. Add `ContainersConfig`, `Test<Name>ServiceApplication`, the test `config/application.yaml` (config client and Eureka client disabled), a context-load test and a `<Name>PersistenceTests` like `CatalogPersistenceTests` as described above.
7. Add `springdoc-openapi-starter-webmvc-api` and an `OpenApiConfig` (copied from an existing service) whose server is `${bazaar.gateway-url}`. In bazaar-config's `api-gateway.yaml`, add a `springdoc.swagger-ui.urls` entry with `url: /<name>-service/v3/api-docs`; the gateway's `ApiDocsRouteConfig` already routes it.
8. Update the services table and the gateway routing table in `README.md`.
