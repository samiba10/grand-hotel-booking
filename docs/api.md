# API

Interactive docs (Swagger UI, with an **Authorize** button for the JWT):

- booking: http://localhost:8082/swagger-ui.html
- auth: http://localhost:8081/swagger-ui.html
- pricing / fx (internal): http://localhost:8083/swagger-ui.html , http://localhost:8084/swagger-ui.html

All public calls go through the gateway at `http://localhost:8080`.

## Required endpoints

| Method | Path | Permission | Success | Errors |
|---|---|---|---|---|
| GET | `/api/rooms/availability?checkin=&checkout=[&roomType=]` | `room:read` | 200 | 400, 401, 503 |
| POST | `/api/bookings` | `booking:create:own\|any` | 201 | 400, 403, 404, 409, 503 |
| GET | `/api/bookings/{id}` | `booking:read:own\|any` | 200 | 403, 404 |
| PUT | `/api/bookings/{id}` | `booking:update:own\|any` | 200 | 400, 403, 404, 409 |
| DELETE | `/api/bookings/{id}` | `booking:cancel:own\|any` | 204 | 403, 404, 409 |
| GET | `/api/guests/{id}/bookings` | `booking:read:own\|any` | 200 | 403, 404 |

## Supporting endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/login` | demo login -> JWT + profile with permissions |
| GET | `/api/auth/me` | current user + permissions (drives FE RBAC) |
| GET | `/api/guests/{id}` | guest profile for the dashboard |
| GET | `/api/audit` | audit trail by transaction / session / entity (ADMIN) |

## Conventions

- **DELETE is a soft cancel**: status becomes `CANCELLED`, the row and its audit history stay, the
  room is released. A second DELETE returns 409.
- **PUT** changes room and/or dates, re-checks availability (excluding the booking itself) and
  re-prices.
- Dates are ISO `yyyy-MM-dd`; a stay is `[checkIn, checkOut)` - back-to-back stays do not overlap.
- Money in responses: `totalAmountMinor` + `currency` (integer minor units).
- Every response has `X-Transaction-Id` and `X-Session-Id` headers.

## Error format (all services and the gateway)

```json
{
  "timestamp": "2026-11-04T10:15:30Z",
  "status": 409,
  "error": "Conflict",
  "message": "Room 101 is already booked for the selected dates",
  "path": "/api/bookings",
  "details": [],
  "transactionId": "BOOKING_CREATE-6f1c2d0e-..."
}
```
