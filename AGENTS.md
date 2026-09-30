# AGENTS.md

Instructions for AI coding agents working in this repository. See `README.md` for the project overview and run options.

## Project

Bazaar is an e-commerce platform built as Spring Boot microservices (Java 25, Spring Boot 4.1, Maven multi-module). Each service owns its PostgreSQL database. Infrastructure and apps run with Docker Compose. Images are built with Cloud Native Buildpacks.

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
- **Entities:** protected no-arg constructor, getters only, static factory methods for creation (e.g. `Product.create(...)`).
- **Loading relationships:** shape the data in the query (map the association, then use a `left join fetch` `@Query`). Don't load flat rows and assemble graphs in Java.
- **Schema:** Flyway migrations in `src/main/resources/db/migration` (`V<n>__<description>.sql`). Hibernate runs with `ddl-auto: validate`, so every schema change needs a migration. Never edit an applied migration.

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
- Each service has `src/test/resources/config/application.yaml` that sets `spring.cloud.config.enabled: false` and `eureka.client.enabled: false`, so tests need no config or discovery server. It holds copies of the bazaar-config settings tests rely on (`server.port`, shared `spring.jpa` / `spring.mvc` settings), but not the datasource, which comes from `@ServiceConnection`. Keep these copies in sync when the shared settings change.
- `config-server`, `discovery-server` and `api-gateway` have no containers, so they only have a plain `@SpringBootTest` context-load test.

## Adding a new microservice

1. Create the module `<name>-service` with the parent `com.rogeriofrsouza:bazaar` and add it to `<modules>` in the root `pom.xml`. Declare `spring-boot-maven-plugin` without extra config; the image settings are inherited. Add `spring-cloud-starter-config` and `spring-cloud-starter-netflix-eureka-client`.
2. Pick the next free app port (`catalog-service` uses 8081). Put `server.port` and the datasource settings in `<name>-service.yaml` in bazaar-config; the local `application.yaml` gets `spring.application.name` and the config-client settings, copied from an existing service.
3. Add `<name>-service-db` to `docker/infra.yml` with its own named volume, healthcheck and a **distinct host port** (`<NAME>_DB_PORT` in `.env`), and use the same port in the localhost default in bazaar-config's `<name>-service.yaml`.
4. Add the app to `docker/apps.yml` with `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` coming from `<NAME>_DB_*` in `docker/.env` plus `CONFIG_SERVER_URL: ${CONFIG_SERVER_URL}` and `DISCOVERY_SERVER_URL: ${DISCOVERY_SERVER_URL}`. It `depends_on` its database with `condition: service_healthy` and on `config-server` and `discovery-server` with `condition: service_started`. Set `restart: on-failure` so it restarts until `config-server` is up.
5. Add `ContainersConfig`, `Test<Name>ServiceApplication`, the test `config/application.yaml` (config client and Eureka client disabled) and a context-load test as described above.
6. Update the services table in `README.md`.
