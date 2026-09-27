# AGENTS.md

## Project

Spring Boot fruit-machine service (take-home). Java, Maven. Package root: `com.andredurante.fruitmachine`, split into `domain` (game logic, no Spring), `service`, `api` (REST/DTOs), `config`.

## Build & test

```bash
mvn spring-boot:run
mvn test
```

### Config defaults

Play cost = $1, starting float = $1000, base game = 4 slots / 4 colours / k=2. All five become configurable per machine in Part 3–4 — these are just the values used while building Parts 1–2.

### Decisions already made — don't relitigate these, and don't ask for the brief again, it's all here

- **Adjacency is linear, no wrap.** Slot N and slot 1 are never adjacent. *Matches a real machine's reels read left-to-right, and keeps the win-detection streak counter O(n) without a wrap-around special case.*
- **Jackpot takes exclusive precedence over small-prize.** All-slots-match is checked first and short-circuits — it never also pays the small-prize adjacency tier. *A jackpot spin trivially also satisfies "adjacent slots matching"; paying both would undercut the jackpot as the top tier.*
- **Play cost is deducted before the prize is computed.** Charge first, then evaluate the outcome on the resulting float. *The machine should never pay out, as part of a jackpot, money it just took in on the same play.*
- **Money is integer cents (`long`), and "half the float" rounds down.** *Avoids floating-point bugs; rounding down means the machine never pays out more than the float actually holds.*
- **Free plays from a shortfall are `ceil(shortfall / cost)`.** Applies whenever a won prize exceeds the current float — never applies to a jackpot, since a jackpot pays the whole float by definition and can't fall short. *Rounding up guarantees the player is never under-compensated for a prize they won.*
- **One flat small-prize payout per play**, regardless of how many separate adjacent-matching runs exist in the row once slot count is generalized. *The brief describes "the outcome" of a play as singular, not a stackable per-occurrence bonus.*
- **A play with float below play cost is rejected outright** — a distinct "insufficient float" outcome, no spin, no charge, no state mutation. *A negative float isn't meaningful for "a sum of money," and this is the validation case surfaced as 4xx at the API layer.*
- **Colours are a configurable list per machine, not a fixed enum.** *The brief allows hundreds of colours; an enum can't scale per-machine.*
- **k (small-prize window) is generalized, not hardcoded at 2.** Win-detection must stay O(n) in slot count — a running streak counter, not a nested loop.
- **Machine state lives in an in-memory `Map<id, FruitMachine>`** behind a `MachineRepository` interface, so swapping to a real datastore later only touches the repository implementation, not the service. No persistence across restarts — documented in the README, not solved.

### Conventions

- `domain` and `service` packages have zero Spring annotations — testable in plain JUnit, no `@SpringBootTest` needed for rules logic.
- `Random` is always constructor-injected, never `new Random()` inside a method.
- Validation happens in the API layer (`@Valid` / a dedicated validator) before the service is called — the service assumes valid input, it doesn't re-check it.

### Out of scope (see README "what I left out")

Concurrency control on a single machine's state, auth, rate limiting — unless the README says otherwise.
