package com.grandhotel.pricing.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class QuoteDtos {

  private QuoteDtos() {}

  /**
   * @param occupancyPercent hotel occupancy for the stay, supplied by booking-service (pricing has
   *     no access to bookings - no circular dependency)
   */
  public record QuoteRequest(
      @NotNull LocalDate checkIn,
      @NotNull LocalDate checkOut,
      @Min(0) @Max(100) int occupancyPercent,
      @NotEmpty Set<String> roomTypes,
      @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency) {}

  public record QuoteResponse(String currency, FxInfo fx, List<RoomTypeQuote> quotes) {}

  public record FxInfo(String base, String quote, long rateMicros, Instant asOf, boolean stale) {}

  public record RoomTypeQuote(
      String roomType, String currency, long totalMinor, List<NightlyPrice> nights) {}

  public record NightlyPrice(
      LocalDate date, long amountMinor, long seasonMultiplierBp, long occupancyMultiplierBp) {}
}
