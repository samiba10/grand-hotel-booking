package com.grandhotel.common.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grandhotel.common.exception.ForbiddenException;
import com.grandhotel.common.security.Role;
import com.grandhotel.common.security.VerifiedToken;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthzTest {

  private final PermissionResolver resolver =
      new ConfigPermissionResolver(
          new AuthzProperties(
              Map.of(
                  "GUEST", Set.of("booking:read:own"),
                  "STAFF", Set.of("booking:read:any"),
                  "ADMIN", Set.of("booking:read:any", "audit:read"))));
  private final AccessGuard guard = new AccessGuard();

  @Test
  void permissionsAreTheUnionOfRolePermissions() {
    assertThat(resolver.permissionsFor(Set.of(Role.GUEST, Role.ADMIN)))
        .containsExactlyInAnyOrder("booking:read:own", "booking:read:any", "audit:read");
  }

  @Test
  void ownScopeAllowsOnlyOwnResources() {
    var guest = resolver.toUser(new VerifiedToken("dana", Set.of(Role.GUEST), 1L));

    assertThat(guard.can(guest, Permissions.BOOKING_READ, 1L)).isTrue();
    assertThat(guard.can(guest, Permissions.BOOKING_READ, 2L)).isFalse();
    assertThatThrownBy(() -> guard.require(guest, Permissions.BOOKING_READ, 2L))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void anyScopeAllowsEveryResource() {
    var staff = resolver.toUser(new VerifiedToken("sam", Set.of(Role.STAFF), null));
    assertThat(guard.can(staff, Permissions.BOOKING_READ, 99L)).isTrue();
  }

  @Test
  void accountWithoutGuestProfileOwnsNothing() {
    var odd = resolver.toUser(new VerifiedToken("x", Set.of(Role.GUEST), null));
    assertThat(guard.can(odd, Permissions.BOOKING_READ, null)).isFalse();
  }
}
