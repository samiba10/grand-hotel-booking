package com.grandhotel.gateway.security;

import static com.grandhotel.gateway.TestFixtures.ERRORS;
import static com.grandhotel.gateway.TestFixtures.JWT;
import static com.grandhotel.gateway.TestFixtures.PERMISSIONS;
import static com.grandhotel.gateway.TestFixtures.PROPS;
import static org.assertj.core.api.Assertions.assertThat;

import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.common.security.Role;
import com.grandhotel.gateway.context.RequestContextGlobalFilter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class EdgeSecurityFiltersTest {

  private final JwtAuthGlobalFilter authn = new JwtAuthGlobalFilter(JWT, PERMISSIONS, ERRORS, PROPS);
  private final RouteAuthorizationGlobalFilter authz = new RouteAuthorizationGlobalFilter(ERRORS, PROPS);

  private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
  private final GatewayFilterChain sink =
      exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
      };
  /** AuthN then AuthZ, as in production. */
  private final GatewayFilterChain authzThenSink = exchange -> authz.filter(exchange, sink);

  private static String token(String user, Role role) {
    return "Bearer " + JWT.issue(user, Set.of(role), role == Role.GUEST ? 1L : null).token();
  }

  @Test
  void publicPathPassesWithoutToken() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login"));
    authn.filter(exchange, authzThenSink).block();
    assertThat(forwarded.get()).isNotNull();
  }

  @Test
  void protectedPathWithoutTokenIs401() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/bookings/1"));
    authn.filter(exchange, authzThenSink).block();
    assertThat(forwarded.get()).isNull();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void validTokenIsForwardedWithResolvedPermissions() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/bookings/1")
                .header(HttpHeaders.AUTHORIZATION, token("dana", Role.GUEST)));
    authn.filter(exchange, authzThenSink).block();

    AuthenticatedUser user = forwarded.get().getAttribute(RequestContextGlobalFilter.USER_ATTR);
    assertThat(user.permissions()).contains("booking:read:own");
    assertThat(forwarded.get().getRequest().getHeaders().getFirst(JwtAuthGlobalFilter.USER_HEADER))
        .isEqualTo("dana");
  }

  @Test
  void spoofedIdentityHeaderIsStripped() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/auth/login").header(JwtAuthGlobalFilter.USER_HEADER, "admin"));
    authn.filter(exchange, authzThenSink).block();
    assertThat(forwarded.get().getRequest().getHeaders().getFirst(JwtAuthGlobalFilter.USER_HEADER))
        .isNull();
  }

  @Test
  void routeRequiringAPermissionTheUserLacksIs403() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/audit")
                .header(HttpHeaders.AUTHORIZATION, token("dana", Role.GUEST)));
    authn.filter(exchange, authzThenSink).block();
    assertThat(forwarded.get()).isNull();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void routeRequiringAPermissionTheUserHasPasses() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/audit")
                .header(HttpHeaders.AUTHORIZATION, token("boss", Role.ADMIN)));
    authn.filter(exchange, authzThenSink).block();
    assertThat(forwarded.get()).isNotNull();
  }
}
