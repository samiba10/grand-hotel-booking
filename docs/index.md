# Grand Hotel Booking Platform

A focused hotel booking platform for a single property ("Grand Hotel", id 1): room availability
search with dynamic pricing, booking management, and a guest dashboard.

**Priorities, in the assignment's order:** code quality and architectural decisions over feature
count. Each design choice is explained where it lives and summarised in [Decisions](decisions.md).

| Area | Where |
|---|---|
| Services, request flow, data model (diagrams) | [Architecture](architecture.md) |
| AuthN vs AuthZ layers, permission matrix, FE RBAC | [Security](security.md) |
| Transaction id + session id, AuditLog | [Auditing](auditing.md) |
| Dynamic pricing, FX, integer money | [Pricing & FX](pricing-and-fx.md) |
| Endpoints, Swagger | [API](api.md) |
| Tests and coverage | [Testing](testing.md) |
| Load test method and results | [Performance](performance.md) |
| How the 5 hours were spent | [Time management](time-management.md) |

## Assignment coverage

| Requirement | Status |
|---|---|
| Static $100/night (assumption) | ✅ as the base price; dynamic on top (Pricing bonus) - see README notes |
| RESTful API, proper status codes | ✅ 200/201/204/400/401/403/404/409/429/503 |
| ORM + PostgreSQL | ✅ JPA/Hibernate instead of Entity Framework (ADR 1), Flyway migrations |
| Validation + error handling | ✅ Bean Validation + one `ApiError` contract (with transaction id) |
| Authentication middleware | ✅ JWT at the gateway **and** in every service |
| 6 required endpoints | ✅ see [API](api.md) |
| Rooms / Guests / Bookings / AuditLog schema | ✅ see [Architecture](architecture.md#data-model) |
| React: `/search`, `/booking/:id`, `/guest/:id` | ✅ responsive, validation, loading states, React Router |
| README, Swagger, architecture | ✅ |
| Bonus: Docker + compose | ✅ (treated as mandatory) |
| Bonus: API Gateway | ✅ Spring Cloud Gateway |
| Bonus: health checks | ✅ actuator liveness/readiness, used by compose |
| Bonus: structured logging + correlation ids | ✅ transaction id + session id on every log line |
| Bonus: pricing microservice | ✅ season x occupancy, + mocked FX service |
| Bonus: Hebrew + RTL | ✅ Tailwind logical properties |
| Bonus: RBAC (Guest / Staff / Admin) | ✅ BE and FE |
| Bonus: rate limiting | ✅ token bucket at the edge |
| Bonus: integration tests | ✅ MockMvc through the real security chain, concurrency test |
| Bonus: performance testing | ✅ k6, documented results |
| Bonus: CI/CD | ✅ GitHub Actions |
