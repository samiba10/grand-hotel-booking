package com.grandhotel.common.security.servlet;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Per-service endpoints reachable without a token. Everything else requires a valid JWT. */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(List<String> publicPaths) {

  public SecurityProperties {
    publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
  }
}
