package com.grandhotel.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TokenBucketRateLimiterTest {

  private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

  private TokenBucketRateLimiter limiter(int capacity, double refillPerSecond, int maxClients) {
    return new TokenBucketRateLimiter(
        new RateLimitProperties(true, capacity, refillPerSecond, maxClients, false), clock);
  }

  @Test
  void allowsBurstUpToCapacityThenRejects() {
    var limiter = limiter(3, 1, 100);

    assertThat(limiter.tryAcquire("1.1.1.1").allowed()).isTrue();
    assertThat(limiter.tryAcquire("1.1.1.1").allowed()).isTrue();
    assertThat(limiter.tryAcquire("1.1.1.1").allowed()).isTrue();

    var rejected = limiter.tryAcquire("1.1.1.1");
    assertThat(rejected.allowed()).isFalse();
    assertThat(rejected.retryAfterSeconds()).isEqualTo(1);
  }

  @Test
  void refillsOverTime() {
    var limiter = limiter(1, 2, 100);
    assertThat(limiter.tryAcquire("c").allowed()).isTrue();
    assertThat(limiter.tryAcquire("c").allowed()).isFalse();

    clock.advance(Duration.ofMillis(600)); // 2 tokens/s -> 1.2 tokens

    assertThat(limiter.tryAcquire("c").allowed()).isTrue();
  }

  @Test
  void clientsAreIsolated() {
    var limiter = limiter(1, 1, 100);
    assertThat(limiter.tryAcquire("attacker").allowed()).isTrue();
    assertThat(limiter.tryAcquire("attacker").allowed()).isFalse();

    assertThat(limiter.tryAcquire("legit-user").allowed()).isTrue();
  }

  @Test
  void trackedClientsAreBoundedToProtectMemory() {
    var limiter = limiter(5, 1, 10);
    for (int i = 0; i < 1_000; i++) {
      limiter.tryAcquire("spoofed-" + i);
    }
    assertThat(limiter.trackedClients()).isLessThanOrEqualTo(10);
  }

  /** Test clock that can be moved forward. */
  static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant start) {
      this.now = start;
    }

    void advance(Duration d) {
      now = now.plus(d);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
