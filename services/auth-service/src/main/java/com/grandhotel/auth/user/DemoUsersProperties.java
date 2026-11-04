package com.grandhotel.auth.user;

import com.grandhotel.common.security.Role;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * "Mock Authentication: simple JWT with hardcoded users" (assignment). Users live in configuration,
 * passwords overridable by environment variables. A real IdP replaces this whole service.
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoUsersProperties(List<DemoUser> users) {

  public DemoUsersProperties {
    users = users == null ? List.of() : List.copyOf(users);
  }

  /**
   * @param guestId the guest profile (booking-service) this account owns; null for staff
   */
  public record DemoUser(
      String username, String password, String displayName, Set<Role> roles, Long guestId) {}
}
