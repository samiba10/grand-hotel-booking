package com.grandhotel.common.security;

import java.util.Objects;
import java.util.Set;

/** Caller identity (AuthN) enriched with resolved permissions (AuthZ). The security principal. */
public record AuthenticatedUser(
    String username, Set<Role> roles, Long guestId, Set<String> permissions) {

  public AuthenticatedUser {
    roles = Set.copyOf(roles);
    permissions = Set.copyOf(permissions);
  }

  public boolean hasPermission(String permission) {
    return permissions.contains(permission);
  }

  public boolean hasRole(Role role) {
    return roles.contains(role);
  }

  public boolean ownsGuest(Long id) {
    return guestId != null && Objects.equals(guestId, id);
  }
}
