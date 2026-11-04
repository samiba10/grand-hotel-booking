package com.grandhotel.pricing.engine;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Pricing rules as configuration - business can tune them without a code change. All numbers are
 * integers: prices in minor units, multipliers in basis points (10000 = x1.00).
 *
 * @param baseCurrency currency the hotel prices in (ILS); quotes are converted via fx-service
 * @param basePrices per room type, minor units of baseCurrency per night
 */
@ConfigurationProperties(prefix = "app.pricing")
public record PricingProperties(
    String baseCurrency,
    Map<String, Long> basePrices,
    List<Season> seasons,
    List<OccupancyTier> occupancyTiers) {

  public PricingProperties {
    basePrices = basePrices == null ? Map.of() : Map.copyOf(basePrices);
    seasons = seasons == null ? List.of() : List.copyOf(seasons);
    occupancyTiers = occupancyTiers == null ? List.of() : List.copyOf(occupancyTiers);
  }

  /**
   * Recurring yearly window, inclusive, as {@code MM-dd}. May wrap the year end (12-20 .. 01-05).
   */
  public record Season(String name, String from, String to, long multiplierBasisPoints) {}

  /** Applies when occupancy is at least {@code minPercent}. The highest matching tier wins. */
  public record OccupancyTier(int minPercent, long multiplierBasisPoints) {}
}
