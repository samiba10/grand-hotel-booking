# Changelog

Documentation is updated in the same commit as the code it describes.
| Step | Code | Docs |
|---|---|---|
| 1 | Project skeleton: root POM, ignore files | README, docs site, decisions, time management |
| 2 | common-lib: JWT (AuthN), permission resolver + AccessGuard (AuthZ), request context (tx/session ids), Money, ApiError, auto-configuration | security, auditing |
| 3 | service-parent (shared servlet deps) + auth-service: demo users, BCrypt, login, /me with permissions | README demo users |
| 4 | fx-service: rates as BIGINT micros with history, pluggable provider (mock random walk), no rate constant in code | - |
| 5 | pricing-service: per-night price = base x season x occupancy, FX conversion, stale fallback / 503 | pricing & FX |
