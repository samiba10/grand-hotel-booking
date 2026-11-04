package com.grandhotel.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-client token bucket settings.
 *
 * @param enabled master switch
 * @param capacity burst size (max requests in a spike)
 * @param refillPerSecond sustained requests per second
 * @param maxTrackedClients upper bound on remembered clients, so the limiter itself cannot be used
 *     to exhaust memory with spoofed / rotating IPs
 * @param trustForwardedFor use the first X-Forwarded-For hop as client key; enable ONLY behind a
 *     trusted load balancer that overwrites the header
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
    boolean enabled,
    int capacity,
    double refillPerSecond,
    int maxTrackedClients,
    boolean trustForwardedFor) {

  public RateLimitProperties {
    if (capacity <= 0 || refillPerSecond <= 0 || maxTrackedClients <= 0) {
      throw new IllegalStateException("app.rate-limit values must be positive");
    }
  }
}
