package com.grandhotel.booking.pricing;

import com.grandhotel.booking.room.RoomType;
import com.grandhotel.common.exception.ServiceUnavailableException;
import com.grandhotel.common.money.Money;
import java.time.Instant;
import java.util.Map;

/** Totals per room type for one stay, plus the FX rate they were computed with. */
public record PricingQuote(
    String currency,
    Map<RoomType, Long> totalsMinor,
    long fxRateMicros,
    Instant fxAsOf,
    boolean fxStale) {

  public Money totalFor(RoomType type) {
    Long minor = totalsMinor.get(type);
    if (minor == null) {
      throw new ServiceUnavailableException("No price returned for " + type, null);
    }
    return Money.of(minor, currency);
  }
}
