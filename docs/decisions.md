# Decisions

Short ADRs: context -> decision -> consequence.

### 1. Java / Spring Boot + JPA instead of .NET Entity Framework
The assignment names Entity Framework; the team's stack and my experience are Java. JPA/Hibernate
is the Java equivalent (ORM, migrations via Flyway, LINQ-like derived queries). Same concepts,
production-grade, no time lost learning a stack under a 5-hour box.

### 2. Microservices split by business capability
`booking` owns rooms, guests, bookings and audit, because the no-double-booking rule must be
checked in ONE database transaction. `pricing` and `fx` are separate because they change for
different reasons (pricing rules, market data). `auth` is separate because it will be replaced by
a real IdP. No Eureka: Docker DNS gives service discovery for free.

### 3. AuthN and AuthZ are separate layers; permissions, not roles
See [Security](security.md). Adding a role is configuration; replacing the IdP or the permission
source touches one class each.

### 4. Pessimistic row lock against double booking
`SELECT ... FOR UPDATE` on the room, then overlap check, then insert, in one transaction.
Optimistic locking would need retries exactly in the contended case that matters ("last room").
The lock is per room, so different rooms never block each other. Pricing (a remote call) is done
BEFORE taking the lock, so the lock is held only for milliseconds. Verified by a concurrency test
and by k6 under contention on PostgreSQL.

### 5. DELETE = soft cancel
A hotel never forgets a booking: status `CANCELLED`, row and audit trail kept, room released.

### 6. Integer money
BIGINT minor units, basis-point multipliers, micro-unit FX rates. See [Pricing & FX](pricing-and-fx.md).

### 7. Pricing receives occupancy instead of reading bookings
Avoids a circular dependency (booking -> pricing -> booking) and keeps pricing a pure function.

### 8. Token relay between services
Downstream services authorize the real end user, not a "trusted service" identity.

### 9. In-memory rate limiter
Correct for one gateway instance. With replicas: Spring Cloud Gateway's Redis `RequestRateLimiter`
(same token-bucket semantics, shared state).
