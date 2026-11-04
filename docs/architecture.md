# Architecture

## Components

```mermaid
flowchart LR
  U[Browser] --> FE[React SPA<br/>Tailwind, EN/HE RTL]
  FE --> SDK[FE API SDK<br/>token, session id,<br/>error mapping]
  SDK -->|HTTPS /api| GW

  subgraph Edge
    GW[gateway-service<br/>1 request context<br/>2 rate limit<br/>3 AuthN<br/>4 AuthZ coarse<br/>5 route]
  end

  GW -->|/api/auth/**| AUTH[auth-service<br/>demo users, JWT]
  GW -->|/api/rooms, bookings,<br/>guests, audit| BK[booking-service<br/>AuthN again, AuthZ fine,<br/>AuditLog]
  BK -->|token relay + tx/session ids| PR[pricing-service<br/>season x occupancy]
  PR -->|token relay| FX[fx-service mock<br/>rates + history]
  BK --> DB1[(bookingdb)]
  FX --> DB2[(fxdb)]

  classDef internal stroke-dasharray: 5 5
  class PR,FX internal
```

`pricing-service` and `fx-service` are **internal**: the gateway has no route to them.

| Module | Responsibility | Port (local) |
|---|---|---|
| `libs/common-lib` | AuthN, AuthZ, request context, `Money`, error model - auto-configured | - |
| `gateway-service` | Single entry point | 8080 |
| `auth-service` | Mock IdP: login, JWT, `/me` with permissions | 8081 |
| `booking-service` | Rooms, guests, bookings, availability, audit | 8082 |
| `pricing-service` | Dynamic price per night | 8083 |
| `fx-service` | Exchange rates (mock provider) | 8084 |
| `frontend` | React SPA | 5173 dev / 3000 docker |

## Request path

```mermaid
sequenceDiagram
  autonumber
  participant FE as React + SDK
  participant GW as Gateway
  participant BK as booking-service
  participant PR as pricing-service
  participant FX as fx-service
  participant DB as bookingdb

  FE->>GW: POST /api/bookings<br/>Authorization, X-Session-Id
  GW->>GW: new tx id BOOKING_CREATE-uuid, keep/issue session id
  GW->>GW: rate limit, AuthN (JWT), AuthZ (route needs booking:*)
  GW->>BK: forward + X-Transaction-Id, X-Transaction-Type, X-Session-Id
  BK->>BK: AuthN again (defense in depth), @PreAuthorize (coarse)
  BK->>BK: AccessGuard: own guest or booking:create:any (fine)
  BK->>PR: POST /api/pricing/quotes (same JWT, same tx id)
  PR->>FX: GET /api/fx/rates/latest (same JWT, same tx id)
  FX-->>PR: rateMicros, asOf
  PR-->>BK: total per room type (integer minor units)
  BK->>DB: SELECT room FOR UPDATE, overlap check, INSERT booking + AuditLog (one tx)
  BK-->>GW: 201 + X-Transaction-Id
  GW-->>FE: 201 + X-Transaction-Id + X-Session-Id
  Note over GW,DB: every log line in every service carries tx=... sid=... user=...
```

## Data model

```mermaid
erDiagram
  HOTELS ||--o{ ROOMS : has
  ROOMS ||--o{ BOOKINGS : "booked as"
  GUESTS ||--o{ BOOKINGS : makes
  HOTELS {
    bigint id PK "1 = Grand Hotel"
    varchar name
  }
  ROOMS {
    bigint id PK
    bigint hotel_id FK
    varchar room_number
    varchar room_type "STANDARD|DELUXE|SUITE"
    varchar status "ACTIVE|MAINTENANCE"
  }
  GUESTS {
    bigint id PK
    varchar first_name
    varchar last_name
    varchar email UK
    varchar phone
  }
  BOOKINGS {
    bigint id PK
    bigint guest_id FK
    bigint room_id FK
    date check_in
    date check_out
    varchar status "CONFIRMED|CANCELLED"
    bigint total_amount_minor
    varchar currency
    bigint fx_rate_micros
    timestamptz created_at
    timestamptz updated_at
  }
  AUDIT_LOG {
    bigint id PK
    varchar action
    varchar entity_type
    varchar entity_id
    varchar user_id
    timestamptz timestamp
    varchar transaction_id
    varchar transaction_type
    varchar session_id
    int outcome
    varchar details
  }
  FX_RATES {
    bigint id PK
    varchar base_currency
    varchar quote_currency
    bigint rate_micros
    timestamptz as_of
    varchar source
  }
```

`FX_RATES` lives in `fxdb` (database per service - no cross-service joins). Users are not in a
table: the assignment asks for hardcoded demo users; `guestId` in the JWT links an account to its
guest profile.

## How cross-cutting code is shared

| Concern | Written once in |
|---|---|
| Versions, plugins, coverage | root `pom.xml` |
| Servlet service dependencies | `services/service-parent/pom.xml` |
| JWT, permission resolution, `AccessGuard` | `common-lib` (auto-configuration) |
| Security filter chain, error handler, request context filter | `common-lib` |
| Role -> permission mapping, logging pattern, DB switch | `common-lib/common-defaults.yml` |
| Date rules for stays | `StayDates` (availability, create, update) |
| Container build | root `Dockerfile` with `SERVICE` build arg |
| Edge policy (public paths, route permissions, transaction types) | gateway `application.yml` |

