package com.grandhotel.auth.user;

import com.grandhotel.common.security.Role;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** In-memory user store. Passwords are BCrypt-hashed at startup - never kept in plain text. */
@Component
public class UserDirectory {

  private final Map<String, UserRecord> users;

  public UserDirectory(DemoUsersProperties props, PasswordEncoder encoder) {
    this.users =
        props.users().stream()
            .map(
                u ->
                    new UserRecord(
                        u.username(),
                        encoder.encode(u.password()),
                        u.displayName(),
                        u.roles(),
                        u.guestId()))
            .collect(Collectors.toUnmodifiableMap(u -> key(u.username()), Function.identity()));
  }

  public Optional<UserRecord> find(String username) {
    return username == null ? Optional.empty() : Optional.ofNullable(users.get(key(username)));
  }

  private static String key(String username) {
    return username.toLowerCase(Locale.ROOT);
  }

  public record UserRecord(
      String username, String passwordHash, String displayName, Set<Role> roles, Long guestId) {}
}
