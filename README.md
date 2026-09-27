# FruitSlots

Java 21 / Spring Boot 4.1.1 application skeleton, generated with [Spring Initializr](https://start.spring.io/). Core spin logic selects one of four colours for each of four slots. A jackpot occurs when all four colours match.

## Run locally

Run commands from the repository root. Requires JDK 21; a separate Maven installation is not needed. The Maven wrapper downloads Maven and dependencies on first use.

```sh
./mvnw spring-boot:run
```

The application listens on port 8080. Stop it with **Ctrl+C**.

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Test and build

Run the automated tests:

```sh
./mvnw test
```

The current tests cover Spring context startup and repository lookup, save, and replacement behavior. Domain tests use a mocked random source to cover matching and mixed colours, successive spins, and immutable four-slot outcomes.

Run a clean build, including tests and executable JAR packaging:

```sh
./mvnw clean verify
```

Run the packaged application:

```sh
java -jar target/fruitmachine-0.0.1-SNAPSHOT.jar
```

## Health check

```sh
curl -i http://localhost:8080/health
```

With the application running, execute this in another terminal, or open [the health endpoint](http://localhost:8080/health) in a browser. Returns HTTP 200 with `{"status":"UP"}`. This is a basic application liveness endpoint, with no external dependency checks.

## Packages

Base package: `com.andredurante.fruitmachine`.

- `domain`: machine identity, colours, slots, spin outcomes, and core spin logic.
- `service`: machine service and repository interface.
- `storage`: in-memory repository implementation.
- `api`: HTTP endpoints, including the health check.
- `config`: future application configuration.

Dependencies: `spring-boot-starter-web`, `spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-actuator`, and `spring-boot-starter-test` (test scope).

## Core spin

```java
FruitMachine machine = new FruitMachine(new Random());
SpinOutcome outcome = machine.spin();
boolean jackpot = outcome.isJackpot();
```

`FruitMachine`, `SpinOutcome`, `Slot`, and `Colour` live in the domain package; `Random` is `java.util.Random`. The caller supplies the random source once. Each spin returns an immutable outcome with exactly four slots, each showing BLACK, WHITE, GREEN, or YELLOW. The spin engine is not yet exposed through an HTTP endpoint or connected to stored machine identities. Payout logic is not implemented.

## Extension seams

`MachineService` receives `MachineRepository` through constructor injection. The sole implementation, `InMemoryMachineRepository`, stores immutable `Machine` records by UUID in a `ConcurrentHashMap`. Saving inserts or replaces by ID; an unknown ID returns an empty `Optional`. Data is lost on restart. A future datastore adapter should replace the in-memory Spring bean while preserving the interface.

Springdoc and Actuator use their dependency defaults; no custom annotations, configuration, logging, or metrics code have been added.

## Docker

Requires Docker with its daemon running. Local Java and Maven installations are not required for this path.

```sh
docker build -t fruitmachine .
docker run --rm -p 8080:8080 fruitmachine
```

The multi-stage image runs `mvn package` (including tests) with Maven and JDK 21, then copies the executable JAR into a JRE 21 Alpine image. Check `http://localhost:8080/health` as above. Stop with **Ctrl+C**; `--rm` removes the stopped container while retaining the image.

Run either the local application or the container on port 8080. If that port is already occupied, use `-p 8081:8080` and check `http://localhost:8081/health` instead. Rebuild the image after source changes.

The initial Docker skeleton was verified locally: its build and three tests passed, and the running container's `/health` endpoint returned HTTP 200 with `{"status":"UP"}`. Rebuild to include the core spin changes.

## Continuous integration

[The GitHub Actions workflow](.github/workflows/ci.yml) runs `mvn test` with Java 21 on pushes and pull requests. It has no deployment step or build matrix. View results in the repository's **Actions** tab after pushing; a hosted CI run has not yet been verified.

Keep the POM, Docker image tags, and CI Java version aligned when upgrading Java.

## AI usage

See [AI_USAGE.md](AI_USAGE.md) for prompts, resulting changes, and verification. Entries are added only when explicitly requested; this is a manual project log.
