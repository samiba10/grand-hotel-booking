package com.grandhotel.gateway.ratelimit;

import com.grandhotel.gateway.FilterOrder;
import com.grandhotel.gateway.web.ErrorResponseWriter;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * DDoS / abuse protection plugin: per-client rate limiting at the edge, executed BEFORE any JWT
 * crypto so floods are rejected as cheaply as possible. Answers 429 + {@code Retry-After}.
 *
 * <p>Complements (does not replace) network-level protection: request-size limit (default filter in
 * application.yml), connection timeouts, and a CDN/WAF in front of production.
 */
@Component
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true")
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

  static final String REMAINING_HEADER = "X-RateLimit-Remaining";

  private final TokenBucketRateLimiter limiter;
  private final ErrorResponseWriter errors;
  private final boolean trustForwardedFor;

  @Autowired
  public RateLimitGlobalFilter(RateLimitProperties props, ErrorResponseWriter errors) {
    this(new TokenBucketRateLimiter(props, Clock.systemUTC()), errors, props.trustForwardedFor());
  }

  RateLimitGlobalFilter(
      TokenBucketRateLimiter limiter, ErrorResponseWriter errors, boolean trustForwardedFor) {
    this.limiter = limiter;
    this.errors = errors;
    this.trustForwardedFor = trustForwardedFor;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var decision = limiter.tryAcquire(clientKey(exchange.getRequest()));
    HttpHeaders headers = exchange.getResponse().getHeaders();
    headers.set(REMAINING_HEADER, String.valueOf(decision.remaining()));

    if (!decision.allowed()) {
      headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
      return errors.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
    }
    return chain.filter(exchange);
  }

  String clientKey(ServerHttpRequest request) {
    if (trustForwardedFor) {
      String xff = request.getHeaders().getFirst("X-Forwarded-For");
      if (xff != null && !xff.isBlank()) {
        return xff.split(",")[0].trim();
      }
    }
    return Optional.ofNullable(request.getRemoteAddress())
        .map(InetSocketAddress::getAddress)
        .map(java.net.InetAddress::getHostAddress)
        .orElse("unknown");
  }

  @Override
  public int getOrder() {
    return FilterOrder.RATE_LIMIT;
  }
}
