# AI Usage Log

Record AI-assisted project requests in chronological order only when the user explicitly asks for an entry. Include the date, tool, prompt, changes, and validation results. Keep prompts verbatim where practical, but redact credentials or sensitive data. This log is maintained manually.

## 2026-09-27 — Initial project setup

**Tool:** OpenAI Codex

**User prompt:**

> Set up a java maven Spring Boot project (use spring initializr if possible) with dependencies: spring-boot-starter-web, spring-boot-starter-validation, spring-boot-starter-test. Package structure com.\<name>.fruitmachine with domain, service, api, config packages. Don't write any game logic yet just the skeleton, build file, and a health check endpoint.
>
> Add an AI Usage log for this project so we can track what's been prompted

**Changes:** Generated a Java 21 Maven project using Spring Initializr. Selected `com.andredurante.fruitmachine` based on the workspace username. Normalized the generated Spring Boot version to the published Maven Central version `4.1.1` and adjusted starters to the three explicitly requested dependencies. Added the four packages, `GET /health`, Maven wrapper, setup documentation, and this log. No game logic added.

**Validation:** `./mvnw -B clean verify` passed, including the generated Spring context test and executable JAR packaging. Started the packaged application on localhost port 18080 and verified `GET /health` returned HTTP 200 with `{"status":"UP"}`, then stopped it. Build and localhost verification required elevated sandbox permissions for the Maven cache/network and socket binding.

## 2026-09-27 — Extension seams

**Tool:** OpenAI Codex

**User prompt:**

> Prepare this Maven Spring Boot project so  API docs, Docker, CI, persistence, observability)can be added later without restructuring. Don't implement any of them yet, just the seams:
>
> 1. Add `springdoc-openapi-starter-webmvc-ui` and `spring-boot-starter-actuator` to the pom as dependencies. Don't add any OpenAPI annotations or actuator config — just make them available.
> 2. Define a `MachineRepository` interface (`findById`, `save`, `existsById`) in the `service` (or a new `storage`) package, with a single `InMemoryMachineRepository` implementation backed by a `ConcurrentHashMap`. The service should depend on the interface, not the map directly — that's the only change needed later to swap in a real datastore.
> 3. Add a minimal multi-stage `Dockerfile`: build stage with a Maven+JDK image running `mvn package`, run stage copying the jar into a slim JRE image, `ENTRYPOINT` running it. Don't add compose or anything beyond a single runnable container.
> 4. Add a GitHub Actions workflow at .github/workflows/ci.yml that checks out the repo, sets up the same JDK version as the pom, and runs `mvn test` on push and pull_request. Nothing else — no deploy step, no matrix builds.

> Leave logging as whatever Spring Boot gives by default for now, don't add structured logging or metrics code, that's a later decision once there's play/payout logic to actually observe.

**Changes:** Added Springdoc 3.1.1 and Spring Boot Actuator dependencies without custom configuration or annotations. Added an identity-only immutable `Machine(UUID id)`, the `MachineRepository` service interface, a single `ConcurrentHashMap`-backed storage implementation, and a constructor-injected `MachineService` that delegates through the interface. Added a Java 21 multi-stage Dockerfile and a GitHub Actions workflow that runs `mvn test` on push and pull requests. Updated the README with storage semantics and Docker commands. No game logic, custom logging, or metrics code added.

**Validation:** `./mvnw -B clean verify` passed: 3 tests, 0 failures/errors, including Spring context startup and repository missing-ID, save, and replacement behavior. Executable JAR packaging succeeded. `git diff --check` passed.

## Entry template

- Date:
- AI tool:
- User prompt:
- Changes made:
- Validation and outcome:
