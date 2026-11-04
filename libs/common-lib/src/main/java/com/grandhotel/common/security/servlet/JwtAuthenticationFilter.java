package com.grandhotel.common.security.servlet;

import com.grandhotel.common.authz.PermissionResolver;
import com.grandhotel.common.context.RequestContext;
import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.common.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Service-side AuthN (defense in depth: a service never trusts that the gateway was in front of
 * it). Step 1 verifies the token (AuthN), step 2 resolves permissions (AuthZ) - two separate,
 * replaceable components. Granted authorities = permissions + ROLE_x, so endpoints use
 * {@code @PreAuthorize("hasAuthority('booking:create:own')")}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final PermissionResolver permissions;

  public JwtAuthenticationFilter(JwtService jwtService, PermissionResolver permissions) {
    this.jwtService = jwtService;
    this.permissions = permissions;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    JwtService.extractBearer(request.getHeader(HttpHeaders.AUTHORIZATION))
        .flatMap(jwtService::verify)
        .map(permissions::toUser)
        .ifPresent(JwtAuthenticationFilter::authenticate);
    chain.doFilter(request, response);
  }

  private static void authenticate(AuthenticatedUser user) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    user.roles().forEach(r -> authorities.add(new SimpleGrantedAuthority(r.authority())));
    user.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
    var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);
    MDC.put(RequestContext.MDC_USER, user.username());
  }
}
