package com.grandhotel.fx.provider;

/**
 * Where rates come from. The mock below simulates a market; a real implementation (e.g. a central
 * bank API) implements this interface and nothing else changes.
 */
public interface FxRateProvider {

  /**
   * @param lastKnownMicros the latest stored rate, used by providers that only publish deltas
   * @return the new rate in micros of {@code quote} per 1 {@code base}
   */
  long fetchRateMicros(String base, String quote, long lastKnownMicros);

  String name();
}
