package com.grandhotel.gateway;

import com.grandhotel.common.authz.AuthzProperties;
import com.grandhotel.common.authz.ConfigPermissionResolver;
import com.grandhotel.common.security.JwtProperties;
import com.grandhotel.common.security.JwtService;
import com.grandhotel.gateway.GatewayProperties.RoutePermission;
import com.grandhotel.gateway.GatewayProperties.TransactionType;
import com.grandhotel.gateway.web.ErrorResponseWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.json.JsonMapper;

public final class TestFixtures {

  public static final JwtService JWT =
      new JwtService(
          new JwtProperties("gateway-test-secret-gateway-test-secret", Duration.ofMinutes(5), "t"));

  public static final ConfigPermissionResolver PERMISSIONS =
      new ConfigPermissionResolver(
          new AuthzProperties(
              Map.of(
                  "GUEST", Set.of("room:read", "booking:read:own"),
                  "ADMIN", Set.of("room:read", "booking:read:any", "audit:read"))));

  public static final GatewayProperties PROPS =
      new GatewayProperties(
          List.of("/api/auth/login"),
          List.of(new RoutePermission("/api/audit/**", List.of("audit:read"))),
          List.of(new TransactionType("POST", "/api/bookings", "BOOKING_CREATE")));

  public static final ErrorResponseWriter ERRORS =
      new ErrorResponseWriter(JsonMapper.builder().build());

  private TestFixtures() {}
}
