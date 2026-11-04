package com.grandhotel.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

/**
 * AuthN primitive: issues and verifies JWTs. Framework-agnostic (no servlet / reactive types), so
 * it is shared by the reactive gateway and the servlet services. Swapping to an external IdP (OIDC)
 * means replacing this class only - AuthZ is untouched.
 */
public class JwtService {

  static final String ROLES_CLAIM = "roles";
  static final String GUEST_CLAIM = "gid";

  private final SecretKey key;
  private final JwtProperties props;
  private final Clock clock;

  public JwtService(JwtProperties props) {
    this(props, Clock.systemUTC());
  }

  public JwtService(JwtProperties props, Clock clock) {
    this.props = props;
    this.clock = clock;
    this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
  }

  public IssuedToken issue(String username, Collection<Role> roles, Long guestId) {
    Instant now = clock.instant();
    Instant expiresAt = now.plus(props.expiration());
    var builder =
        Jwts.builder()
            .subject(username)
            .issuer(props.issuer())
            .claim(ROLES_CLAIM, roles.stream().map(Role::name).sorted().toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt));
    if (guestId != null) {
      builder.claim(GUEST_CLAIM, guestId);
    }
    return new IssuedToken(builder.signWith(key).compact(), expiresAt);
  }

  /** Returns the verified identity, or empty for an invalid / expired / foreign token. */
  public Optional<VerifiedToken> verify(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer(props.issuer())
              .clock(() -> Date.from(clock.instant()))
              .build()
              .parseSignedClaims(token)
              .getPayload();
      Number gid = claims.get(GUEST_CLAIM, Number.class);
      return Optional.of(
          new VerifiedToken(
              claims.getSubject(), rolesOf(claims), gid == null ? null : gid.longValue()));
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  /** Extracts the raw token from an {@code Authorization: Bearer <token>} header value. */
  public static Optional<String> extractBearer(String authorizationHeader) {
    String prefix = "Bearer ";
    if (authorizationHeader == null || !authorizationHeader.startsWith(prefix)) {
      return Optional.empty();
    }
    return Optional.of(authorizationHeader.substring(prefix.length()).trim());
  }

  private static Set<Role> rolesOf(Claims claims) {
    List<?> raw = claims.get(ROLES_CLAIM, List.class);
    if (raw == null || raw.isEmpty()) {
      return EnumSet.noneOf(Role.class);
    }
    return raw.stream()
        .map(Object::toString)
        .map(Role::valueOf)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
  }

  public record IssuedToken(String token, Instant expiresAt) {}
}
