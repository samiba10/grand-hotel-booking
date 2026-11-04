# Grand Hotel Booking Platform

Hotel booking platform built as microservices: **Spring Boot 4 + Spring Cloud Gateway** backend,
**React + TypeScript + Tailwind** frontend (English / Hebrew RTL), PostgreSQL, Docker.

## Run it (Docker - the supported way)

Prerequisites: Docker Desktop. Nothing else.

```bash
cp .env.example .env        # optional: real secrets
docker compose up --build   # first build ~5 min, then seconds
```

| What | URL |
|---|---|
| Web app | http://localhost:3000 |
| API (gateway) | http://localhost:8080 |
| Swagger - booking | http://localhost:8082/swagger-ui.html |
| Swagger - auth / pricing / fx | :8081 / :8083 / :8084 `/swagger-ui.html` |

Demo users (dev only):

| User | Password | Role | Can |
|---|---|---|---|
| `guest1` | `Guest123!` | GUEST (Dana Levi, guest #1) | search, book / view / edit / cancel **own** bookings |
| `guest2` | `Guest123!` | GUEST (John Smith, guest #2) | same, for guest #2 |
| `staff` | `Staff123!` | STAFF | everything above for **any** guest |
| `admin` | `Admin123!` | ADMIN | staff + **audit log** |

```bash
./scripts/smoke-test.sh     # end-to-end API check (needs curl + jq)
```

## Develop without Docker

Java 21, Maven 3.9+, Node 20+.

```bash
mvn install -DskipTests     # once: installs common-lib for single-module runs
mvn verify                  # all tests + coverage reports
```

Start `fx-service`, `pricing-service`, `booking-service`, `auth-service`, `gateway-service` with
`mvn -pl services/<name> spring-boot:run` (H2 in memory, no database needed).

## Documentation

Everything else is in [`docs/`](docs/index.md) (served with `make docs`): architecture diagrams,
AuthN / AuthZ design, auditing, pricing & FX, API, tests, performance results, decisions.
