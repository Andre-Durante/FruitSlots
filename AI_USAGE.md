# AI Usage Log

Record each AI-assisted project request in chronological order. Include the date, tool, prompt, changes, and validation results. Keep prompts verbatim where practical, but redact credentials or sensitive data. This log is maintained manually.

## 2026-09-27 — Initial project setup

**Tool:** OpenAI Codex

**User prompt:**

> Set up a java maven Spring Boot project (use spring initializr if possible) with dependencies: spring-boot-starter-web, spring-boot-starter-validation, spring-boot-starter-test. Package structure com.\<name>.fruitmachine with domain, service, api, config packages. Don't write any game logic yet just the skeleton, build file, and a health check endpoint.
>
> Add an AI Usage log for this project so we can track what's been prompted

**Changes:** Generated a Java 21 Maven project using Spring Initializr. Selected `com.andredurante.fruitmachine` based on the workspace username. Normalized the generated Spring Boot version to the published Maven Central version `4.1.1` and adjusted starters to the three explicitly requested dependencies. Added the four packages, `GET /health`, Maven wrapper, setup documentation, and this log. No game logic added.

**Validation:** `./mvnw -B clean verify` passed, including the generated Spring context test and executable JAR packaging. Started the packaged application on localhost port 18080 and verified `GET /health` returned HTTP 200 with `{"status":"UP"}`, then stopped it. Build and localhost verification required elevated sandbox permissions for the Maven cache/network and socket binding.

## Entry template

- Date:
- AI tool:
- User prompt:
- Changes made:
- Validation and outcome:
