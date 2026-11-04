package com.grandhotel.gateway;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Edge policy as configuration.
 *
 * @param publicPaths reachable without a JWT
 * @param routePermissions coarse AuthZ: caller needs ANY of the listed permissions for the route
 * @param transactionTypes names each kind of request; the name prefixes its transaction id
 */
@ConfigurationProperties(prefix = "app.gateway")
public record GatewayProperties(
    List<String> publicPaths,
    List<RoutePermission> routePermissions,
    List<TransactionType> transactionTypes) {

  public GatewayProperties {
    publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    routePermissions = routePermissions == null ? List.of() : List.copyOf(routePermissions);
    transactionTypes = transactionTypes == null ? List.of() : List.copyOf(transactionTypes);
  }

  public record RoutePermission(String pattern, List<String> anyOf) {}

  public record TransactionType(String method, String pattern, String type) {}
}
