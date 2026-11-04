package com.grandhotel.gateway.security;

import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.gateway.FilterOrder;
import com.grandhotel.gateway.GatewayProperties;
import com.grandhotel.gateway.context.RequestContextGlobalFilter;
import com.grandhotel.gateway.web.ErrorResponseWriter;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

/**
 * Coarse AuthZ at the edge: "may this identity call this route AT ALL?" (e.g. only audit readers
 * reach /api/audit). Cheap, and it keeps obviously-forbidden traffic away from services.
 *
 * <p>The fine decision - "THIS booking?" - needs the resource, so it is made in the service
 * ({@code AccessGuard}). Routes without a rule are left to the service.
 */
@Component
public class RouteAuthorizationGlobalFilter implements GlobalFilter, Ordered {

  private final ErrorResponseWriter errors;
  private final List<Rule> rules;

  public RouteAuthorizationGlobalFilter(ErrorResponseWriter errors, GatewayProperties props) {
    this.errors = errors;
    this.rules =
        props.routePermissions().stream()
            .map(r -> new Rule(PathPatternParser.defaultInstance.parse(r.pattern()), r.anyOf()))
            .toList();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    Object attr = exchange.getAttribute(RequestContextGlobalFilter.USER_ATTR);
    if (!(attr instanceof AuthenticatedUser user)) {
      return chain.filter(exchange); // public route: AuthN filter let it through
    }
    var path = exchange.getRequest().getPath().pathWithinApplication();
    for (Rule rule : rules) {
      if (rule.pattern().matches(path)) {
        boolean allowed = rule.anyOf().stream().anyMatch(user::hasPermission);
        return allowed
            ? chain.filter(exchange)
            : errors.write(exchange, HttpStatus.FORBIDDEN, "You do not have permission for this action");
      }
    }
    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return FilterOrder.AUTHORIZATION;
  }

  private record Rule(PathPattern pattern, List<String> anyOf) {}
}
