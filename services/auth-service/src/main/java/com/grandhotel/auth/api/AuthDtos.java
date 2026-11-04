package com.grandhotel.auth.api;

import com.grandhotel.common.security.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public final class AuthDtos {

  private AuthDtos() {}

  public record LoginRequest(
      @NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 72) String password) {}

  public record UserProfile(
      String username,
      String displayName,
      Set<Role> roles,
      Set<String> permissions,
      Long guestId,
      String region) {}

  public record LoginResponse(String token, Instant expiresAt, UserProfile user) {}
}
