package com.grandhotel.common.authz;

import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.common.security.Role;
import com.grandhotel.common.security.VerifiedToken;
import java.util.Collection;
import java.util.Set;

/**
 * AuthZ extension point: turns roles into permissions. The default implementation reads
 * configuration; a database- or policy-engine-backed (e.g. OPA) implementation can replace it by
 * declaring its own bean.
 */
public interface PermissionResolver {

  Set<String> permissionsFor(Collection<Role> roles);

  default AuthenticatedUser toUser(VerifiedToken token) {
    return new AuthenticatedUser(
        token.username(), token.roles(), token.guestId(), permissionsFor(token.roles()));
  }
}
