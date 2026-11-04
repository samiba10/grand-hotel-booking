# Getting started

## Docker (recommended)

```bash
docker compose up --build
./scripts/smoke-test.sh
```

The stack: `postgres`, `fx-service`, `pricing-service`, `booking-service`, `auth-service`,
`gateway-service`, `frontend`. Compose starts them in dependency order and waits for each
service's readiness probe (`/actuator/health/readiness`).

Logs of one request across all services:

```bash
docker compose logs | grep "<transaction id from the X-Transaction-Id response header>"
```

## Local, without Docker

```bash
mvn install -DskipTests
mvn -pl services/fx-service spring-boot:run        # :8084
mvn -pl services/pricing-service spring-boot:run   # :8083
mvn -pl services/booking-service spring-boot:run   # :8082
mvn -pl services/auth-service spring-boot:run      # :8081
mvn -pl services/gateway-service spring-boot:run   # :8080
cd frontend && npm install && npm run dev           # :5173
```

Each service uses an in-memory H2 database by default; `DB_URL` switches to PostgreSQL.

!!! warning "Changed common-lib?"
    Run `mvn install -DskipTests` again, otherwise single-module runs keep using the old jar from
    `~/.m2`.

## Configuration

| Variable | Default | Used by |
|---|---|---|
| `JWT_SECRET` | dev-only value | all services (>= 32 bytes, startup fails otherwise) |
| `DB_URL` | H2 in memory | booking, fx |
| `PRICING_URL` / `FX_URL` | localhost | booking / pricing |
| `RATE_LIMIT_ENABLED` | `true` | gateway |
| `FX_REFRESH_INTERVAL` | `PT30S` | fx |
