# Time management

The assignment's time-management section, filled in with how the work was actually split.

| Hour | Focus | Delivered |
|---|---|---|
| 1 - Foundation | repo, build, shared library, Docker skeleton, docs skeleton | root POM, `common-lib` (AuthN, AuthZ, request context, `Money`, errors), compose, MkDocs |
| 2 - Backend core | booking-service + auth | schema + migrations, 6 endpoints, ownership AuthZ, double-booking lock, AuditLog, Swagger, tests |
| 3 - Pricing + edge | pricing, fx, gateway | dynamic pricing, mock FX, gateway filters (tx/session ids, rate limit, AuthN, AuthZ) |
| 4 - Frontend core | React app | SDK, RBAC layer, `/search`, `/booking/:id`, `/guest/:id`, EN/HE RTL, validation, loading states |
| 5 - Polish | quality + delivery | full Docker run, k6 benchmarks, coverage, docs, demo |

## Trade-offs made for the time box

| Not done | Why it is OK here | Next step |
|---|---|---|
| Real IdP | assignment asks for mock auth with hardcoded users | OIDC (Keycloak / Entra ID): swap `JwtService` |
| Redis cache | not needed at measured load | cache availability quotes + FX |
| Message queue | no async workflow required yet | outbox -> RabbitMQ for notifications |
| Event sourcing / CQRS | AuditLog covers the audit need | event store if replay is required |
