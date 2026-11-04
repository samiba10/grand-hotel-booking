package com.grandhotel.auth.api;

import com.grandhotel.auth.api.AuthDtos.LoginRequest;
import com.grandhotel.auth.api.AuthDtos.LoginResponse;
import com.grandhotel.auth.api.AuthDtos.UserProfile;
import com.grandhotel.auth.user.AuthService;
import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.common.security.servlet.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/login")
  @Operation(summary = "Login with a demo user; returns a JWT and the user's permissions")
  public LoginResponse login(@Valid @RequestBody LoginRequest req) {
    return auth.login(req);
  }

  @GetMapping("/me")
  @Operation(summary = "Current user, roles and resolved permissions (drives FE RBAC)")
  public UserProfile me(@CurrentUser AuthenticatedUser user) {
    return auth.me(user);
  }
}
