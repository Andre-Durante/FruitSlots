# AI Usage Log

Record AI-assisted project requests in chronological order only when the user explicitly asks for an entry. Include the date, tool, prompt, changes, and validation results. Keep prompts verbatim where practical, but redact credentials or sensitive data. This log is maintained manually.

## 2026-09-27 — Initial project setup

**Tool:** OpenAI Codex

**User prompt:**

> Set up a java maven Spring Boot project (use spring initializr if possible) with dependencies: spring-boot-starter-web, spring-boot-starter-validation, spring-boot-starter-test. Package structure com.\<name>.fruitmachine with domain, service, api, config packages. Don't write any game logic yet just the skeleton, build file, and a health check endpoint.

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

## 2026-09-27 — Core spin and jackpot (Part 1)

**Tool:** OpenAI Codex

**User prompt:**

> **Core spin & jackpot (Part 1):**
>
> Four slots, each showing one of four colours (BLACK, WHITE, GREEN, YELLOW). Write a Colour/Slot domain model and a FruitMachine class with spin() that randomly picks a colour per slot and returns an outcome object, plus a jackpot check: true when all four slots match. Take a Random as a constructor dependency, don't instantiate it inside spin(). Write JUnit tests using a seeded/mocked random: jackpot detected when all four match, not detected otherwise.

**Changes:** Added `Colour`, immutable `Slot` and `SpinOutcome` records, and `FruitMachine` with constructor-injected `Random`. Each spin independently selects four colours; `SpinOutcome.isJackpot()` checks whether all four match. Outcomes defensively copy their slots and require exactly four entries. Added deterministic JUnit tests using mocked random values and updated the README to describe the spin engine. The engine remains separate from repository identities and HTTP endpoints; no payout logic added.

**Validation:** `./mvnw -B test` passed all 17 tests (14 domain cases plus 3 existing tests), with zero failures or errors. Coverage includes jackpots for every colour, a mismatch in every slot position, all four colours in order, successive spins, immutable outcomes, and invalid slot counts. The initial sandbox run failed because Mockito could not attach its JVM agent; the rerun with elevated permissions passed. `git diff --check` passed.

## 2026-09-27 — Payouts and float (Part 2)

**Tool:** OpenAI Codex

**User prompt (summary):** Extend `FruitMachine` with a $1 fixed play cost and $1,000 starting float. Charge the float before evaluation. Jackpot (all colours equal) pays the entire float; full house (all colours different) pays half; small prize (two or more adjacent equal colours, fixed k=2) pays five times the cost. Implement seven resolved decisions exactly and record each with its reasoning in README: linear adjacency without wrapping; exclusive jackpot precedence; cost deducted before prize evaluation; integer cents (`long`) with half-float rounded down; shortfall free-play credit of `ceil(shortfall / cost)` (never needed for jackpot); one flat small-prize payout regardless of run count; and insufficient-float rejection without spinning, charging, or state mutation. Write explicit tests for every decision and add the work to AI usage.

**Changes:** Added the float, fixed cost, and `play()` settlement to `FruitMachine`, plus `PrizeTier` and immutable `PlayOutcome`. The default balance is 100,000 cents; an overload permits an existing non-negative balance. Prize evaluation short-circuits by tier. Cash payout is capped at the remaining float and unpaid amounts produce rounded-up per-play credits. Added full-house and linear adjacent-pair checks to `SpinOutcome`. Preserved `spin()` as slot generation. Updated README with payout rules, usage examples, and one decision-and-reason bullet per requested decision. Free-play redemption and HTTP integration remain outside this part.

**Validation:** `./mvnw -B test` passed all 31 tests, with zero failures/errors, including 14 payout test cases. Explicit tests cover matching row ends without adjacency, jackpot exclusivity, post-charge jackpot calculation, half-cent rounding, a 251-cent shortfall, two distinct matching runs paying once, and rejection below cost without random calls or mutation. Additional cases cover exact shortfalls, three-slot runs, a balance equal to cost, repeated play after depletion, initial balance, and negative-balance rejection. `git diff --check` passed.

## 2026-09-27 — Generalise and scale

**Tool:** OpenAI Codex

**User prompt:**

> **Generalise & scale**
>
> Generalise the machine: N slots, M colours (potentially hundreds), and a small-prize window k (not fixed at 2), same rules otherwise. My current small-prize check is an O(n·k) nested loop — show me how to make it O(n) with a running streak counter instead, without touching the payout logic. Also: with hundreds of colours, should the colour set stay a fixed enum, or become a configurable list/id per machine? Argue both sides in two sentences before you touch code.


**Changes:** Compared enum and configurable palette options before editing. Replaced the fixed enum with a colour-ID value record and added immutable per-machine `MachineConfiguration` for N, palette (M), and k, preserving the four-slot/four-colour/k=2 defaults. Spins sample the configured palette and produce N slots. Replaced the fixed adjacent-pair predicate with a running streak counter; the previous k=2 implementation was already O(n), and the generalised check remains O(n) time/O(1) auxiliary space. Matching uses colour-ID value equality. Payout calculations and settlement are unchanged. Updated README with examples, configuration boundaries, decisions, and the algorithm explanation.

**Validation:** `./mvnw -B test` passed all 40 tests, including the unchanged payout suite and 11 generalisation tests. Coverage includes 500 colours, configurable slots and k, no wrapping or combining runs, k=1 and k=N, value equality, palette immutability, invalid configurations, generalized prize precedence, and a 150,000-slot row. Removed the old invalid-length cases for 3 and 5 slots, which are now valid. `git diff --check` passed.

## 2026-09-27 — REST layer

**Tool:** OpenAI Codex

**User prompt:**

> **REST layer**
>
> Wrap FruitMachine in a Spring Boot REST API: POST /machines/{id}/plays (spin, return outcome + float + free plays credited), GET /machines/{id} (current float and config), POST /machines (configure: slot count, colours, k, play cost, starting float). Game logic stays in a plain service class with no Spring annotations — the web layer only translates HTTP to it. State lives in an in-memory map keyed by id; note in a comment that it isn't persisted across restarts. Validate: positive slot count, k <= slot count, at least 2 distinct colours, non-negative cost/float — map failures to 400 with a clear message.


**Changes:** Added creation, retrieval, and play REST endpoints with DTOs and API-boundary validation. Removed Spring annotations from the service and wired it through configuration beans with an injected random factory. Replaced identity-only repository values with actual `FruitMachine` state, keyed by UUID, and documented loss on restart in code and README. Made play cost configurable; zero-cost plays avoid shortfall division when no shortfall exists. Creation returns 201/Location, unknown IDs return 404, invalid input returns 400/message, and insufficient float returns 400 with its distinct outcome. Added strict integer parsing and an upper cost bound ensuring 5x cost fits in long cents. Updated README with curl examples, response fields, validation, and limitations.

**Validation:** `./mvnw -B test` passed all 54 tests, including 14 real HTTP integration cases. Tests exercise create/get/play/state retrieval, custom-cost shortfall credits, depleted float, zero-cost plays, malformed JSON/IDs, unknown IDs, missing and invalid fields, duplicate/insufficient palettes, fractional cents, and overflow bounds. Existing game/payout tests pass. `git diff --check` passed.

## 2026-09-28 — Review edge cases without patches

**Tool:** OpenAI Codex

**User prompt:**

> **Poke holes, don't patch yet**
>
> Review FruitMachine and the REST layer as a strict reviewer. List edge cases I'm missing. free plays interacting with a play that itself can't be paid, a machine configured with one colour, k=1, concurrent plays hitting the same machine id. Don't fix anything, just list them with one line each on why they matter.


**Review findings:** Free-play credits are returned but not stored or redeemable, and shortfall settlement empties the float so the next positive-cost play is rejected. A one-colour palette is rejected by REST but accepted by the domain and guarantees jackpots. k=1 eliminates no-prize outcomes; N=1 guarantees jackpots; k=N makes the small-prize tier unreachable due to jackpot precedence; N>M makes full house impossible. Concurrent plays and reads are not atomic. Retried POST requests can create additional plays/charges. Slot count, palette size, and machine count have no application-level resource caps. A failure after cost deduction can leave state changed without a successful response. Zero-cost play is allowed even at zero float. Direct domain callers can bypass API cost bounds and trigger invalid arithmetic. Colour IDs distinguish case and whitespace. Some are intentional rules or documented exclusions, not implementation defects.

**Changes:** AI usage log only; no application code or tests changed.

**Validation:** Static inspection of domain, service, and REST code; no tests run for this review. `git diff --check` passed.

## 2026-09-28 — Store free plays and make generation precede settlement

**Tool:** OpenAI Codex

**User prompt (summary):** Fix only three reviewed issues: store and consume free-play credits before paid plays; reject play cost <= 0 in configuration validation; generate and classify the full spin before charging or applying payouts. Document the remaining review cases as “Expected behaviour, not a bug” (one colour, k=1, k=N, one slot, N>M, validated-input trust) and “Left out, would add with more time” (concurrency, intermediate GETs, POST retries, resource bounds), one line each with consequences/reasoning or proposed remedies.

**Changes:** Added a machine-level free-play counter, consumed only after successful generation/classification and replenished by shortfall awards. Credits bypass the paid-play float check and cost charge. GET machine includes the remaining `freePlays`; play responses retain the newly credited count. API cost validation now requires a strictly positive value. Split outcome classification from financial settlement, retaining post-charge payout computation for paid plays. Updated README semantics and the two requested review groups. No concurrency, idempotency, resource-limit, colour, or k-rule fixes were made.

**Validation:** `./mvnw -B test` passed 61 tests with zero failures/errors. Regression coverage includes persisted HTTP credits, exhaustion and subsequent paid-play rejection, zero-cost rejection, zero-float acceptance, free plays awarding new credits, and generation exceptions preserving cash and credits. `git diff --check` passed.

## Entry template

- Date:
- AI tool:
- User prompt:
- Changes made:
- Validation and outcome:
