package com.grandhotel.gateway.context;

import com.grandhotel.gateway.GatewayProperties;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/** Maps METHOD + path to a business name such as {@code BOOKING_CREATE}. First match wins. */
@Component
public class TransactionTypeResolver {

  private final List<Rule> rules;

  public TransactionTypeResolver(GatewayProperties props) {
    this.rules =
        props.transactionTypes().stream()
            .map(
                t ->
                    new Rule(
                        HttpMethod.valueOf(t.method().toUpperCase(Locale.ROOT)),
                        PathPatternParser.defaultInstance.parse(t.pattern()),
                        t.type()))
            .toList();
  }

  public String resolve(HttpMethod method, PathContainer path) {
    return rules.stream()
        .filter(r -> r.method().equals(method) && r.pattern().matches(path))
        .map(Rule::type)
        .findFirst()
        .orElse("HTTP_" + method.name());
  }

  private record Rule(HttpMethod method, PathPattern pattern, String type) {}
}
