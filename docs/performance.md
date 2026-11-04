# Performance

## Method

Tool: [k6](https://k6.io), run in Docker on the compose network against the gateway (the full
path: gateway -> booking -> pricing -> fx -> PostgreSQL). The rate limiter is disabled for the run
(`RATE_LIMIT_ENABLED=false`) - otherwise it (correctly) answers 429 to the single load-generator
IP.

```bash
make up
make perf              # availability search: ramp 0 -> 50 virtual users over 80 s
make perf-contention   # 500 booking attempts from 50 VUs racing for 10 room/date slots
```

Raw summaries are written to `perf/results/*.json`.

## Scenarios

| Scenario | Script | What it proves | Thresholds |
|---|---|---|---|
| Availability search | `perf/k6/availability.js` | read path latency across 3 services + DB | errors < 1%, p95 < 500 ms |
| Booking contention | `perf/k6/booking-contention.js` | correctness under concurrency: exactly 10 x 201, all others 409, zero 5xx | p95 < 800 ms, checks > 99% |

## Results

Environment: MacBook Pro (Intel), Docker Desktop: 12 CPUs, 7.8 GB RAM

| Scenario | Requests | RPS | p50 | p95 | p99 | Errors |
|---|---|---|---|---|---|---|
| Availability (ramp 0 -> 50 VUs, 80 s) | 8,767 | 109 | 31 ms | 79 ms | 113 ms | 0.00% |
| Contention (50 VUs, 500 attempts, 10 slots) | 501 | 122 | 315 ms | 755 ms | ~1.1 s | 0 x 5xx - created = **10**, conflicts = 490 |

## Observations and next steps

- **Correctness under concurrency holds on PostgreSQL**: 500 parallel attempts for 10 room/date
  slots produced exactly 10 bookings and 490 clean 409s - no double booking, no 5xx. k6 reports
  `http_req_failed` 97.8% because it counts every 4xx; the 409 is the correct answer here.
- **Read path is fast end to end**: p95 79 ms across gateway -> booking -> pricing -> fx -> DB.
- The availability run's 109 req/s is bounded by the load shape (0.2 s think time, ramp), not by
  the system: latency stayed flat at 49 concurrent users. Next: a no-think-time run to find the
  saturation point.
- Contention p95 (755 ms) is driven by losers calling pricing before discovering the room is
  taken. Next: a cheap unlocked overlap pre-check to fail fast; keep price -> lock -> re-check for
  correctness.
- Hot-path caching (availability quotes per room type / dates / occupancy band, FX rate in
  pricing) would cut most of the remaining latency.
- Rate limiting: run `make perf` with the limiter on - expect 429 + `Retry-After` after the burst.
