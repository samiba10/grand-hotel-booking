# Time management

The assignment's time-management section, filled in with how the work was actually split.

| Hour | Focus | Delivered |
|---|---|---|
| 1 - Foundation | repo, build, shared library, Docker skeleton, docs skeleton | root POM, `common-lib` (AuthN, AuthZ, request context, `Money`, errors), compose, MkDocs |
| 2 - Backend core | booking-service + auth | schema + migrations, 6 endpoints, ownership AuthZ, double-booking lock, AuditLog, Swagger, tests |
| 3 - Pricing + edge | pricing, fx, gateway | dynamic pricing, mock FX, gateway filters (tx/session ids, rate limit, AuthN, AuthZ) |
| 4 - Frontend core | React app | SDK, RBAC layer, `/search`, `/booking/:id`, `/guest/:id`, EN/HE RTL, validation, loading states |
| 5 - Polish | quality + delivery | full Docker run, k6 benchmarks, coverage, docs, demo |

## Found by integration, fixed in their own PRs

Bugs that unit tests could not catch - each found by running the real stack, fixed in a separate
PR with the cause in the commit message:

| Found by | Bug | Fix |
|---|---|---|
| first `mvn verify` | our filter bean name clashed with Spring MVC's `requestContextFilter` | renamed to `AuditContextFilter` |
| Hibernate `validate` | `CHAR(3)` columns vs `String` -> `VARCHAR` | currency columns `VARCHAR(3)` |
| first `docker compose up` | `RandomGenerator.getDefault()` needs `jdk.random`, absent from the slim JRE | `SplittableRandom` (java.base) |
| smoke test | gateway and service both set `X-Transaction-Id`/`X-Session-Id`; `fetch` joins duplicates -> session ping-pong broke | `DedupeResponseHeader` at the gateway |
| first `make perf` | k6 started before the recreated gateway was healthy | `docker compose up --wait` |

## Trade-offs made for the time box

| Not done | Why it is OK here | Next step |
|---|---|---|
| Real IdP | assignment asks for mock auth with hardcoded users | OIDC (Keycloak / Entra ID): swap `JwtService` |
| Redis cache | not needed at measured load | cache availability quotes + FX |
| Message queue | no async workflow required yet | outbox -> RabbitMQ for notifications |
| Event sourcing / CQRS | AuditLog covers the audit need | event store if replay is required |
