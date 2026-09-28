# FruitSlots

Java 21 / Spring Boot 4.1.1 REST service, bootstrapped with [Spring Initializr](https://start.spring.io/). Core spin logic supports N slots, a per-machine palette of M colour IDs, and a small-prize run length k. Defaults are four slots, four colours, and k=2. A jackpot occurs when all slots match.

## Quick navigation

- [Run locally](#run-locally)
- [Test in Swagger](#test-in-swagger)
- [Automated tests and build](#test-and-build)
- [REST API and curl examples](#rest-api)
- [Payouts and float](#payouts-and-float)
- [Key decisions](#key-decisions)
- [Docker](#docker)
- [What I left out](#what-i-left-out)

## Run locally

Run commands from the repository root. Requires JDK 21; a separate Maven installation is not needed. The Maven wrapper downloads Maven and dependencies on first use.

```sh
./mvnw spring-boot:run
```

The application listens on port 8080. Stop it with **Ctrl+C**.

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Test in Swagger

With the application running, open [Swagger UI](http://localhost:8080/swagger-ui/index.html). The OpenAPI JSON is available at [GET /v3/api-docs](http://localhost:8080/v3/api-docs).

1. Expand **POST /machines** and click **Try it out**.
2. Paste the request below and click **Execute**.
3. Expect **201 Created**. Copy the `id` from the response body.
4. Expand **GET /machines/{id}**, click **Try it out**, paste the ID, and execute. Expect **200** with the current `floatCents` and `config`.
5. Execute **POST /machines/{id}/plays** using the same ID. No request body is needed. Expect **200** with the spin and settlement.
6. GET the machine again: its float should equal the preceding play's `floatCents`. Its original configuration stays unchanged.

```json
{
  "slotCount": 4,
  "colours": ["BLACK", "WHITE", "GREEN", "YELLOW"],
  "k": 2,
  "playCostCents": 100,
  "startingFloatCents": 100000
}
```

A possible first-play response is:

```json
{
  "outcome": "SMALL_PRIZE",
  "slots": ["BLACK", "WHITE", "BLACK", "BLACK"],
  "prizeCents": 500,
  "paidCents": 500,
  "freePlaysCredited": 0,
  "floatCents": 99400
}
```

The adjacent BLACK pair wins one $5 prize: $1,000 − $1 cost − $5 paid = $994. Spins are random, so your colours and prize may differ. Use the deterministic JUnit tests below to verify particular winning patterns rather than repeatedly spinning until one appears.

### Manual validation checklist

For each creation case, start with the sample request and change the specified fields:

| Case | Expected result |
| --- | --- |
| `slotCount: 0` | 400 with validation message |
| `k: 0` or `k: 5` with four slots | 400 |
| One colour, duplicate IDs, or a blank colour ID | 400 |
| Negative `playCostCents` or `startingFloatCents` | 400 |
| Fractional cents, such as `playCostCents: 100.5` | 400 |
| Omit a required field | 400 |
| `startingFloatCents: 99`, `playCostCents: 100` | Creation returns 201; play returns 400 `INSUFFICIENT_FLOAT`; GET still shows 99 |
| Cost 0 | 400 naming `playCostCents`; starting float 0 is still allowed |
| GET or play with an unknown valid UUID | 404 |
| GET with a malformed UUID | 400 |
| Restart the app, then GET an old machine ID | 404: machines are not persisted |

With a positive play cost, a jackpot empties the machine. If no free-play credits remain, subsequent plays return **400** with `INSUFFICIENT_FLOAT`, empty slots, and no charge or payout. Without free-play credits, rejection also occurs at any balance below the cost, not only zero. Create a new machine to continue; there is no refill endpoint.

## Test and build

Run the automated tests:

```sh
./mvnw test
```

The latest verified suite contains **61 tests**, with zero failures or errors. Tests use deterministic random values for game rules; HTTP tests start a real server on a random local port, so a separately running application is not required.

| Test class | Coverage |
| --- | --- |
| `FruitMachineTests` | Spins, jackpot detection, immutable outcomes |
| `FreePlayTests` | Stored credits, consumption, new credits from free plays, generation-failure state preservation |
| `FruitMachinePayoutTests` | All seven payout decisions, rounding, shortfalls, insufficient float |
| `GeneralisedMachineTests` | Configurable slots/colours/k, boundaries, large palettes and rows |
| `InMemoryMachineRepositoryTests` | Missing IDs, saving and replacing machine state |
| `MachineMockMvcTests` | Configure/get/play round trip, unknown-machine 404, invalid-k 400 naming the field |
| `MachineApiTests` | HTTP creation, retrieval, plays, state updates, validation and errors |
| `FruitMachineApplicationTests` | Spring application context startup |

Run a focused suite:

```sh
./mvnw -Dtest=MachineApiTests test
./mvnw -Dtest=MachineMockMvcTests test
./mvnw -Dtest=FruitMachinePayoutTests test
./mvnw -Dtest=GeneralisedMachineTests test
```

Maven reports `BUILD SUCCESS` on success. Detailed test reports are written to `target/surefire-reports/`.

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

- `domain`: colours, slots, spin outcomes, and core spin logic.
- `service`: machine service and repository interface.
- `storage`: in-memory repository implementation.
- `api`: HTTP endpoints, including the health check.
- `config`: Spring bean wiring for the plain service, repository, and random factory.

Dependencies: `spring-boot-starter-web`, `spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-actuator`, and `spring-boot-starter-test` (test scope).

## Core spin

```java
FruitMachine machine = new FruitMachine(new Random());
SpinOutcome outcome = machine.spin();
boolean jackpot = outcome.isJackpot();
```

`FruitMachine`, `SpinOutcome`, `Slot`, and `Colour` live in the domain package; `Random` is `java.util.Random`. The caller supplies the random source once. Each spin returns an immutable outcome with the configured number of slots and colours; the default palette is BLACK, WHITE, GREEN, and YELLOW. The REST API exposes paid plays and stores machine state by UUID. Use `play()` for a charged play and payout settlement; `spin()` remains a slot-generation operation without financial effects.

## REST API

All monetary fields are **integer cents**; 100 means $1. Creation fields are required even where the domain constructors provide defaults.

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| POST | `/machines` | Create a configured machine | 201 + Location header |
| GET | `/machines/{id}` | Read current float and configuration | 200 |
| POST | `/machines/{id}/plays` | Charge, spin, and settle a play | 200 |
| GET | `/health` | Application liveness | 200 |

Start the application, then create a machine:

```sh
curl -i -X POST http://localhost:8080/machines \
  -H 'Content-Type: application/json' \
  -d '{"slotCount":4,"colours":["BLACK","WHITE","GREEN","YELLOW"],"k":2,"playCostCents":100,"startingFloatCents":100000}'
```

Creation returns **201**, a `Location: /machines/{id}` header, and a body containing `id`, `floatCents`, and `config` (slot count, colours, k, play cost, and initial float). Copy the returned ID into these commands:

```sh
curl http://localhost:8080/machines/REPLACE_WITH_ID
curl -X POST http://localhost:8080/machines/REPLACE_WITH_ID/plays
```

GET returns the current balance and original configuration. A successful play returns **200** with `outcome`, `slots` (colour IDs), `prizeCents`, `paidCents`, `freePlaysCredited`, and the remaining `floatCents`. Credits refer to that play's shortfall and are added to the machine's stored `freePlays` counter. GET returns that remaining counter; a play automatically consumes one credit before considering a paid play, skipping the cost even if the float is zero.

| Response field | Meaning |
| --- | --- |
| `outcome` | JACKPOT, FULL_HOUSE, SMALL_PRIZE, NO_PRIZE, or INSUFFICIENT_FLOAT |
| `slots` | Ordered colour IDs; empty for rejected plays |
| `prizeCents` | Prize won before limiting cash to available float |
| `paidCents` | Cash actually paid |
| `freePlaysCredited` | Rounded-up credit for this play's unpaid prize; not a cumulative balance |
| `floatCents` | Machine balance after cost and payout |

All creation fields are required. Validation requires positive slot count, `1 <= k <= slotCount`, at least two distinct non-blank colour IDs (duplicates rejected), a strictly positive cost, and a non-negative starting float. Cost is limited to `Long.MAX_VALUE / 5` so the five-times-cost prize fits in integer cents. Fractional numbers are rejected. Invalid input returns **400** with a clear `message`; malformed UUIDs return **400** and unknown IDs return **404**. Insufficient float returns **400** with the `INSUFFICIENT_FLOAT` outcome, empty slots, zero payout/credits, and unchanged balance.

Zero-cost configuration is rejected with HTTP 400. Free plays use a stored credit, not a zero configured cost; their prizes still follow the normal payout rules and can generate further shortfall credits.

`MachineService` has no Spring annotations. Configuration beans wire its repository and random-source factory; controllers validate and translate HTTP requests, while domain classes retain game rules. The repository holds a `Map<UUID, FruitMachine>` in memory, and **all machines disappear on restart**.

Run HTTP integration tests with `./mvnw -Dtest=MachineApiTests test`.

## Generalised reels and colour IDs

```java
MachineConfiguration configuration = new MachineConfiguration(
        6, List.of(new Colour("RED"), new Colour("BLUE"), new Colour("GOLD")), 3);
FruitMachine machine = new FruitMachine(new Random(), configuration);
PlayOutcome result = machine.play();
```

The palette is an immutable per-machine list; M is its size. IDs are non-blank, case-sensitive strings and must be unique within the palette. `Colour` is a value record: equal IDs match even when represented by different Java objects. The named default colours remain constants for convenience, not an exhaustive enum. Each slot samples uniformly from the configured palette using the injected `Random`.

N must be positive, the palette must be non-empty, and `1 <= k <= N`. k=1 qualifies any spin for the small-prize check, but jackpot and full-house still take precedence. With N=1, jackpot takes precedence. Full house means every slot differs, not that every available colour appears; when N exceeds M, full house is impossible. The domain palette can contain one colour, but the REST API requires at least two distinct colours. API validation runs before the service is called.

### Linear-time small-prize detection

The previous k=2 implementation already scanned adjacent pairs in O(n). For configurable k, keep a running streak rather than rescanning each candidate window:

```java
int streak = 1;
if (streak >= k) return true;
for (int i = 1; i < slots.size(); i++) {
    streak = slots.get(i - 1).colour().equals(slots.get(i).colour())
            ? streak + 1 : 1;
    if (streak >= k) return true;
}
return false;
```

Each neighbouring pair is compared once: **O(n) time and O(1) extra space**, independent of k (treating colour-ID comparison as constant cost). A mismatch resets the streak to one; row ends are never joined, and separate runs do not accumulate. Only qualification changed: prize calculation and float settlement are unchanged. `SpinOutcome` now requires a non-empty row rather than exactly four slots; `FruitMachine` produces exactly its configured N slots.

Run the generalisation tests with `./mvnw -Dtest=GeneralisedMachineTests test`.

## Payouts and float

By default a machine starts with **100,000 cents ($1,000)** and costs **100 cents ($1)** per play; both are configurable at creation. Each accepted `play()` generates and classifies the complete spin before mutating state. It then consumes a free-play credit or subtracts its configured cost, computes the prize on the resulting float, and subtracts the cash payout. The cost is subtracted, not added, as specified.

| Tier (checked in order) | Condition | Prize |
| --- | --- | --- |
| Jackpot | All slot colours match | Entire post-charge float |
| Full house | All slot colours differ | Half the post-charge float, rounded down to cents |
| Small prize | At least one consecutive run of k matching colours | 5 × configured cost, once per play |
| No prize | None of the above | 0 |

```java
FruitMachine machine = new FruitMachine(new Random());
PlayOutcome result = machine.play();
long cashPaid = result.paidCents();
long freePlayCredit = result.freePlays();
long remainingFloat = machine.floatCents();
```

`PlayOutcome` reports a single `PrizeTier`, an optional spin, the nominal prize, cash paid, free plays credited for this play, and the remaining float. An insufficient-float result has no spin and zero prize, cash, and credits. Free-play credits are stored on the machine and returned as newly awarded credits in the settlement; the next play consumes one without charging the float.

The constructor `FruitMachine(Random, long floatCents)` accepts an existing non-negative balance for restoration and boundary tests; that overload uses the default cost. For example, a 349-cent float becomes 249 cents after charging: a small prize pays that 249 cents and awards 3 free plays for the 251-cent shortfall. A 1,001-cent float becomes 901 cents, so a full house pays 450 cents and leaves 451 cents.

## Key decisions

The following decisions govern the implemented payout rules.

- **Adjacency is linear, with no wrap:** the last and first slots are never adjacent. This matches reels read left-to-right and allows an O(n) streak counter without a wrap-around special case.
- **Jackpot takes exclusive precedence over small-prize:** check jackpot first and short-circuit when all slots match. Paying both tiers on one spin would undercut the jackpot as the top tier, so it is checked and short-circuited first.
- **Deduct the play cost before computing a prize:** subtract the cost from the float, then evaluate jackpot, full-house, or small-prize using the remaining balance. This follows the specified accounting order and prevents the jackpot from including money attributed to the same play.
- **Money uses integer cents (`long`), and half the float rounds down:** integer arithmetic avoids floating-point rounding errors, and rounding down prevents paying a fraction more than the available float.
- **Shortfalls award `ceil(shortfall / cost)` free plays:** pay the available cash and credit free plays for the unpaid amount. Jackpot cannot fall short because its prize is the entire available float. Rounding up compensates the full shortfall; rounding down would under-compensate the player.
- **At most one flat small-prize payout per play:** separate adjacent runs do not stack payouts. The brief describes a single outcome tier and does not ask for per-run bonuses.
- **Reject a paid play when no credits remain and the float is below the play cost:** return a distinct insufficient-float outcome without spinning, charging, or mutating state. A negative float is not meaningful, and the distinct outcome maps to HTTP 400 in the API.

- **Use a configurable palette of colour IDs rather than an enum:** machines can have different palettes containing hundreds of colours without changing application code; value equality gives stable matching semantics.
- **Detect k-length runs with a running streak:** one pass and constant auxiliary space avoid O(n·k) window rescans as slot counts and k grow.

## Extension seams

`MachineService` receives `MachineRepository` through constructor injection. The sole implementation, `InMemoryMachineRepository`, stores `FruitMachine` instances by UUID in a `ConcurrentHashMap`. Saving inserts or replaces by ID; an unknown ID returns an empty `Optional`. Data is lost on restart. A future datastore adapter should replace the in-memory Spring bean while preserving the interface.

Springdoc and Actuator use their dependency defaults; no custom annotations, configuration, logging, or metrics code have been added.

## Docker

Requires Docker with its daemon running. Local Java and Maven installations are not required for this path.

```sh
docker build -t fruitmachine .
docker run --rm -p 8080:8080 fruitmachine
```

The multi-stage image runs `mvn package` (including tests) with Maven and JDK 21, then copies the executable JAR into a JRE 21 Alpine image. Check `http://localhost:8080/health` as above. Stop with **Ctrl+C**; `--rm` removes the stopped container while retaining the image.

Run either the local application or the container on port 8080. If that port is already occupied, use `-p 8081:8080` and check `http://localhost:8081/health` instead. Rebuild the image after source changes.

Open the same Swagger URL to test the container. Rebuild after code changes; an older local image will not include new endpoints. Each container starts with an empty machine repository.

## Continuous integration

[The GitHub Actions workflow](.github/workflows/ci.yml) runs `mvn test` with Java 21 on pushes and pull requests. It has no deployment step or build matrix. View results in the repository's **Actions** tab after pushing; a hosted CI run has not yet been verified.

Keep the POM, Docker image tags, and CI Java version aligned when upgrading Java.

## Review boundaries

### Expected behaviour, not a bug

- **One colour:** the domain allows it and every spin jackpots because all slots necessarily match; the REST API still requires at least two distinct colours.
- **k=1:** NO_PRIZE disappears because every slot is a qualifying run of length one, with jackpot/full-house precedence unchanged.
- **k=N:** small-prize is unreachable because a full-row matching run is already a jackpot and the jackpot tier takes precedence.
- **One slot:** every spin jackpots because all slots in a single-slot row match vacuously, so jackpot precedence applies.
- **More slots than colours:** full house is impossible because at least one colour must repeat, violating the all-different rule.
- **Domain trusts validated input:** production construction comes through the API-validated service; cost bounds are enforced there rather than duplicated in the domain.

### Left out, would add with more time

- **Concurrent plays:** racing balance updates can overpay or corrupt the float; add a per-machine lock around a complete play.
- **GET during a play:** reads can observe an intermediate balance; use the same per-machine lock to capture a consistent snapshot.
- **Retried POST requests:** retries can duplicate creation or charge/spin again; add an idempotency key with stored request results.
- **Unbounded sizes/counts:** large slot/colour configurations or unlimited machine creation can exhaust resources; add configuration size caps and a machine-count cap.

## What I left out

Persistence across restarts, authentication, rate limiting, and a refill endpoint also remain outside scope. Free-play counters, like floats, are lost on restart.

## AI usage

See [AI_USAGE.md](AI_USAGE.md) for prompts, resulting changes, and verification. Entries are added only when explicitly requested; this is a manual project log.
