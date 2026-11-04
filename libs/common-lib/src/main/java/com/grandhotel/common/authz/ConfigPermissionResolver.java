package com.grandhotel.common.authz;

import com.grandhotel.common.security.Role;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/** Default {@link PermissionResolver}: union of the permissions configured for each role. */
public class ConfigPermissionResolver implements PermissionResolver {

  private final AuthzProperties props;

  public ConfigPermissionResolver(AuthzProperties props) {
    this.props = props;
  }

  @Override
  public Set<String> permissionsFor(Collection<Role> roles) {
    Set<String> result = new TreeSet<>();
    for (Role role : roles) {
      result.addAll(props.rolePermissions().getOrDefault(role.name(), Set.of()));
    }
    return Set.copyOf(result);
  }
}
