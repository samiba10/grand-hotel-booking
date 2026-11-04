package com.grandhotel.gateway.security;

import com.grandhotel.common.authz.PermissionResolver;
import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.common.security.JwtService;
import com.grandhotel.gateway.FilterOrder;
import com.grandhotel.gateway.GatewayProperties;
import com.grandhotel.gateway.context.RequestContextGlobalFilter;
import com.grandhotel.gateway.web.ErrorResponseWriter;
import java.util.List;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

/**
 * Edge AuthN: rejects requests without a valid JWT before they reach any service. The verified
 * user (with resolved permissions) is stored on the exchange for the AuthZ filter and the access
 * log. Services still re-verify the token themselves (defense in depth).
 *
 * <p>{@code X-Auth-User} sent by a client is always stripped, so it can never be spoofed; the
 * gateway sets it for downstream logging only - services never trust it for decisions.
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

  public static final String USER_HEADER = "X-Auth-User";

  private final JwtService jwtService;
  private final PermissionResolver permissions;
  private final ErrorResponseWriter errors;
  private final List<PathPattern> publicPaths;

  public JwtAuthGlobalFilter(
      JwtService jwtService,
      PermissionResolver permissions,
      ErrorResponseWriter errors,
      GatewayProperties props) {
    this.jwtService = jwtService;
    this.permissions = permissions;
    this.errors = errors;
    this.publicPaths =
        props.publicPaths().stream().map(PathPatternParser.defaultInstance::parse).toList();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request =
        exchange.getRequest().mutate().headers(h -> h.remove(USER_HEADER)).build();

    if (isPublic(request)) {
      return chain.filter(exchange.mutate().request(request).build());
    }

    Optional<AuthenticatedUser> user =
        JwtService.extractBearer(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
            .flatMap(jwtService::verify)
            .map(permissions::toUser);
    if (user.isEmpty()) {
      return errors.write(exchange, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    exchange.getAttributes().put(RequestContextGlobalFilter.USER_ATTR, user.get());
    ServerHttpRequest authenticated =
        request.mutate().header(USER_HEADER, user.get().username()).build();
    return chain.filter(exchange.mutate().request(authenticated).build());
  }

  boolean isPublic(ServerHttpRequest request) {
    if (HttpMethod.OPTIONS.equals(request.getMethod())) {
      return true; // CORS pre-flight never carries credentials
    }
    var path = request.getPath().pathWithinApplication();
    return publicPaths.stream().anyMatch(p -> p.matches(path));
  }

  @Override
  public int getOrder() {
    return FilterOrder.AUTHENTICATION;
  }
}
