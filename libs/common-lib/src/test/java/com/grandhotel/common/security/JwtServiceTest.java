package com.grandhotel.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "test-secret-test-secret-test-secret-123";
  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  private final JwtProperties props = new JwtProperties(SECRET, Duration.ofMinutes(30), "test");
  private final JwtService jwt = new JwtService(props, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void tokenRoundTripsIdentity() {
    var issued = jwt.issue("dana", Set.of(Role.GUEST), 7L);

    var verified = jwt.verify(issued.token()).orElseThrow();

    assertThat(verified.username()).isEqualTo("dana");
    assertThat(verified.roles()).containsExactly(Role.GUEST);
    assertThat(verified.guestId()).isEqualTo(7L);
    assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
  }

  @Test
  void staffTokenHasNoGuestId() {
    var verified = jwt.verify(jwt.issue("sam", Set.of(Role.STAFF), null).token()).orElseThrow();
    assertThat(verified.guestId()).isNull();
  }

  @Test
  void expiredTokenIsRejected() {
    var issued = jwt.issue("dana", Set.of(Role.GUEST), 1L);
    var later = new JwtService(props, Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC));
    assertThat(later.verify(issued.token())).isEmpty();
  }

  @Test
  void tokenSignedWithAnotherKeyIsRejected() {
    var other =
        new JwtService(
            new JwtProperties(
                "another-secret-another-secret-another-1", Duration.ofMinutes(30), "test"),
            Clock.fixed(NOW, ZoneOffset.UTC));
    var forged = other.issue("mallory", Set.of(Role.ADMIN), null);
    assertThat(jwt.verify(forged.token())).isEmpty();
  }

  @Test
  void garbageTokensAreRejected() {
    assertThat(jwt.verify("not.a.jwt")).isEmpty();
    assertThat(jwt.verify("")).isEmpty();
    assertThat(jwt.verify(null)).isEmpty();
  }

  @Test
  void weakSecretIsRefusedAtStartup() {
    assertThatThrownBy(() -> new JwtProperties("short", Duration.ofMinutes(1), "x"))
        .isInstanceOf(IllegalStateException.class);
  }
}
