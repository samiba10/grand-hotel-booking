package com.grandhotel.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT settings. The secret MUST come from JWT_SECRET outside local development. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {

  private static final int MIN_SECRET_BYTES = 32; // HS256 requires >= 256-bit keys

  public JwtProperties {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (256 bits)");
    }
    expiration = expiration == null ? Duration.ofHours(1) : expiration;
    issuer = issuer == null ? "grand-hotel" : issuer;
  }
}
