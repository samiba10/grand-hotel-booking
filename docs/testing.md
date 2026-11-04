# Testing and coverage

```bash
mvn verify          # all backend tests + JaCoCo report per module
cd frontend && npm test -- --coverage
```

Coverage reports: `<module>/target/site/jacoco/index.html` (backend), `frontend/coverage/` (UI).

## What is tested

| Level | Examples |
|---|---|
| Unit (no Spring) | `Money` rounding / overflow, `JwtService` (expiry, foreign key, garbage), role -> permission resolution, `AccessGuard` own vs any, `StayDates`, `PriceCalculator` (per-night seasons, year-end wrap, compounding), mock FX provider bounds, token bucket |
| API / integration (MockMvc through the real security chain + H2) | every endpoint, every status code: 401 / 403 ownership / 404 / 409 double booking / 400 validation / 503 pricing down; audit rows with transaction + session id; tx/session headers |
| Concurrency | 8 threads book the same room at once -> exactly one booking (`ConcurrentBookingTest`) |
| Gateway | AuthN filter, route AuthZ, header spoofing, transaction id cannot be forged, session ping-pong, log-injection guard, CORS, 401 with transaction id |
| Frontend (Vitest + Testing Library) | SDK session ping-pong + error mapping, RBAC own/any, `<Can>`, stay validation, `/search` validation / pricing display / staff-only guest field / hidden Book button, Hebrew switches `dir=rtl` |
| End to end | `scripts/smoke-test.sh` against the Docker stack |
| Load | k6, see [Performance](performance.md) |

Tests share one in-memory database per service, so each test uses its own date window: the suite
passes in any order.

## Results

**99 backend tests**, all green (`mvn verify`), plus the frontend suite (`npm test`).

| Module | Line coverage (own tests) |
|---|---|
| auth-service | 97% (57/59) |
| fx-service | 96% (64/67) |
| gateway-service | 95% (178/187) |
| booking-service | 87% (310/355) |
| pricing-service | 70% (78/111) -> FX client resilience tests added |
| common-lib | 35% (98/277) own unit tests only - see below |

`common-lib` is mostly exercised by the services' integration tests (security chain, request
context filter, exception handler), which per-module reports do not credit. The `coverage-report`
module aggregates everything: `coverage-report/target/site/jacoco-aggregate/index.html`.
