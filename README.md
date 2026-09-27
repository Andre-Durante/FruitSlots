# FruitSlots

Java 21 / Spring Boot 4.1.1 application skeleton, generated with [Spring Initializr](https://start.spring.io/). No game logic is implemented.

## Build and run

Requires JDK 21. The Maven wrapper downloads Maven and dependencies on first use.

```sh
./mvnw clean verify
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Health check

```sh
curl http://localhost:8080/health
```

Returns HTTP 200 with `{"status":"UP"}`. This is a basic application liveness endpoint, with no external dependency checks.

## Packages

Base package: `com.andredurante.fruitmachine`.

- `domain`: future domain model.
- `service`: future application services.
- `api`: HTTP endpoints, including the health check.
- `config`: future application configuration.

Dependencies: `spring-boot-starter-web`, `spring-boot-starter-validation`, and `spring-boot-starter-test` (test scope).

## AI usage

See [AI_USAGE.md](AI_USAGE.md) for prompts, resulting changes, and verification. Append an entry for each future AI-assisted request; this is a manual project log.
