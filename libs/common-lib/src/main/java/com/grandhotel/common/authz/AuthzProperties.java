package com.grandhotel.common.authz;

import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Role -> permissions mapping, declared once in common-defaults.yml and overridable per
 * environment. Keys are role names.
 */
@ConfigurationProperties(prefix = "app.authz")
public record AuthzProperties(Map<String, Set<String>> rolePermissions) {

  public AuthzProperties {
    rolePermissions = rolePermissions == null ? Map.of() : Map.copyOf(rolePermissions);
  }
}
