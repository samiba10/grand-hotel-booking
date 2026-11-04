package com.grandhotel.fx;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param pairs currency pairs kept fresh, as {@code BASE/QUOTE}, e.g. {@code ILS/USD}
 * @param maxStepBasisPoints mock provider: max move per refresh (30 = 0.30%)
 */
@ConfigurationProperties(prefix = "app.fx")
public record FxProperties(List<String> pairs, int maxStepBasisPoints) {

  public FxProperties {
    pairs = pairs == null ? List.of() : List.copyOf(pairs);
    if (maxStepBasisPoints <= 0) {
      maxStepBasisPoints = 30;
    }
  }
}
