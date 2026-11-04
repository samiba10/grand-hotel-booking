package com.grandhotel.auth.user;

import com.grandhotel.auth.api.AuthDtos.LoginRequest;
import com.grandhotel.auth.api.AuthDtos.LoginResponse;
import com.grandhotel.auth.api.AuthDtos.UserProfile;
import com.grandhotel.auth.user.UserDirectory.UserRecord;
import com.grandhotel.common.authz.PermissionResolver;
import com.grandhotel.common.exception.UnauthorizedException;
import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.common.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserDirectory users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final PermissionResolver permissions;
  private final String dummyHash;

  public AuthService(
      UserDirectory users, PasswordEncoder encoder, JwtService jwt, PermissionResolver permissions) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
    this.permissions = permissions;
    this.dummyHash = encoder.encode("timing-attack-dummy");
  }

  public LoginResponse login(LoginRequest req) {
    var user = users.find(req.username());
    // Always run BCrypt, even for unknown users, so response time does not reveal valid usernames.
    boolean matches =
        encoder.matches(req.password(), user.map(UserRecord::passwordHash).orElse(dummyHash));
    if (user.isEmpty() || !matches) {
      log.info("Login failed");
      throw new UnauthorizedException("Invalid username or password");
    }
    UserRecord u = user.get();
    var issued = jwt.issue(u.username(), u.roles(), u.guestId());
    log.info("Login succeeded for {}", u.username());
    return new LoginResponse(issued.token(), issued.expiresAt(), profile(u));
  }

  /** The FE builds its RBAC layer from this: permissions come from the BE, never guessed. */
  public UserProfile me(AuthenticatedUser principal) {
    String displayName =
        users.find(principal.username()).map(UserRecord::displayName).orElse(principal.username());
    return new UserProfile(
        principal.username(),
        displayName,
        principal.roles(),
        principal.permissions(),
        principal.guestId());
  }

  private UserProfile profile(UserRecord u) {
    return new UserProfile(
        u.username(), u.displayName(), u.roles(), permissions.permissionsFor(u.roles()), u.guestId());
  }
}
