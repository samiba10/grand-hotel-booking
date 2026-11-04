package com.grandhotel.common.security;

/**
 * Platform roles. Roles are only a grouping: what a role may DO is defined by its permissions in
 * {@code app.authz.role-permissions} (see {@link com.grandhotel.common.authz.PermissionResolver}).
 */
public enum Role {
  GUEST,
  STAFF,
  ADMIN;

  public String authority() {
    return "ROLE_" + name();
  }
}
