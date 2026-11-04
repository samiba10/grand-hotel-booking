package com.grandhotel.common.authz;

import com.grandhotel.common.exception.ForbiddenException;
import com.grandhotel.common.security.AuthenticatedUser;

/**
 * Fine-grained AuthZ for resources that have an owner (a guest). Rule, for an action such as
 * {@code booking:read}:
 *
 * <ul>
 *   <li>{@code booking:read:any} - allowed for every resource
 *   <li>{@code booking:read:own} - allowed only when the resource belongs to the caller's guest
 * </ul>
 *
 * Coarse checks (does the caller have the permission at all?) happen earlier: at the gateway per
 * route and with {@code @PreAuthorize} per endpoint. This class answers "on THIS resource?".
 */
public class AccessGuard {

  public boolean can(AuthenticatedUser user, String action, Long ownerGuestId) {
    if (user.hasPermission(action + Permissions.ANY)) {
      return true;
    }
    return user.hasPermission(action + Permissions.OWN) && user.ownsGuest(ownerGuestId);
  }

  public void require(AuthenticatedUser user, String action, Long ownerGuestId) {
    if (!can(user, action, ownerGuestId)) {
      throw new ForbiddenException("You are not allowed to " + describe(action) + " this resource");
    }
  }

  private static String describe(String action) {
    int idx = action.indexOf(':');
    return idx < 0 ? action : action.substring(idx + 1);
  }
}
