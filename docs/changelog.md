# Changelog

Documentation is updated in the same commit as the code it describes.
| Step | Code | Docs |
|---|---|---|
| 1 | Project skeleton: root POM, ignore files | README, docs site, decisions, time management |
| 2 | common-lib: JWT (AuthN), permission resolver + AccessGuard (AuthZ), request context (tx/session ids), Money, ApiError, auto-configuration | security, auditing |
| 3 | service-parent (shared servlet deps) + auth-service: demo users, BCrypt, login, /me with permissions | README demo users |
| 4 | fx-service: rates as BIGINT micros with history, pluggable provider (mock random walk), no rate constant in code | - |
| 5 | pricing-service: per-night price = base x season x occupancy, FX conversion, stale fallback / 503 | pricing & FX |
| 6 | booking-service: 6 required endpoints, rooms/guests/bookings/AuditLog schema, own/any AuthZ, room row lock, request + business audit | API |
| 7 | gateway: typed transaction id per request, session ping-pong, rate limit, AuthN, route-level AuthZ, routing (pricing/fx internal) | architecture (components, request sequence, ERD) |
| 8 | coverage-report: JaCoCo aggregate across modules (common-lib is exercised by service tests) | testing (coverage table) |
| 9 | Frontend scaffold (Vite, React, TS, Tailwind, Vitest) + SDK: single API handler, session ping-pong, ApiError with transaction id | - |
| 10 | Frontend RBAC from backend permissions (<Can>, guards, canOn own/any), EN/HE i18n with dir=rtl, RTL-safe style components | security (FE RBAC) |
| 11 | Pages: /search, /booking/:id, /guest/:id, /admin/audit, /login - responsive, validation, loading states | architecture (frontend structure) |
| 12 | Docker: one Dockerfile for all Java services, nginx frontend, compose with Postgres + healthchecks, smoke test | getting started |
