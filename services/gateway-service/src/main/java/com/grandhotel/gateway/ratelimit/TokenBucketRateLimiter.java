package com.grandhotel.gateway.ratelimit;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory token bucket per client key. Lock-free map, per-bucket synchronization.
 *
 * <p>Scope: single gateway instance. With several replicas, switch to Spring Cloud Gateway's Redis
 * {@code RequestRateLimiter} (same token-bucket semantics, shared state) - see docs/decisions.md.
 */
public class TokenBucketRateLimiter {

  private final int capacity;
  private final double refillPerNano;
  private final int maxTrackedClients;
  private final Clock clock;
  private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  public TokenBucketRateLimiter(RateLimitProperties props, Clock clock) {
    this.capacity = props.capacity();
    this.refillPerNano = props.refillPerSecond() / 1_000_000_000d;
    this.maxTrackedClients = props.maxTrackedClients();
    this.clock = clock;
  }

  public Decision tryAcquire(String clientKey) {
    long now = nanos();
    if (buckets.size() >= maxTrackedClients && !buckets.containsKey(clientKey)) {
      evictFullBuckets(now);
    }
    Bucket bucket = buckets.computeIfAbsent(clientKey, k -> new Bucket(capacity, now));
    return bucket.tryConsume(now);
  }

  int trackedClients() {
    return buckets.size();
  }

  /** Buckets that refilled to capacity carry no state worth keeping. */
  private void evictFullBuckets(long now) {
    buckets.entrySet().removeIf(e -> e.getValue().isFull(now));
    if (buckets.size() >= maxTrackedClients) {
      buckets.clear(); // under attack: fail open on memory, never fail closed for everyone
    }
  }

  private long nanos() {
    var now = clock.instant();
    return now.getEpochSecond() * 1_000_000_000L + now.getNano();
  }

  private final class Bucket {
    private double tokens;
    private long lastRefill;

    Bucket(double tokens, long now) {
      this.tokens = tokens;
      this.lastRefill = now;
    }

    synchronized Decision tryConsume(long now) {
      refill(now);
      if (tokens >= 1) {
        tokens -= 1;
        return Decision.allowed((long) tokens);
      }
      long waitSeconds = (long) Math.ceil((1 - tokens) / refillPerNano / 1_000_000_000d);
      return Decision.rejected(Math.max(1, waitSeconds));
    }

    synchronized boolean isFull(long now) {
      refill(now);
      return tokens >= capacity;
    }

    private void refill(long now) {
      long elapsed = Math.max(0, now - lastRefill);
      tokens = Math.min(capacity, tokens + elapsed * refillPerNano);
      lastRefill = now;
    }
  }

  public record Decision(boolean allowed, long remaining, long retryAfterSeconds) {
    static Decision allowed(long remaining) {
      return new Decision(true, remaining, 0);
    }

    static Decision rejected(long retryAfterSeconds) {
      return new Decision(false, 0, retryAfterSeconds);
    }
  }
}
