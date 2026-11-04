package com.grandhotel.pricing.engine;

import com.grandhotel.common.exception.BadRequestException;
import com.grandhotel.common.money.Money;
import com.grandhotel.pricing.engine.PricingProperties.OccupancyTier;
import com.grandhotel.pricing.engine.PricingProperties.Season;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Pure pricing function (no I/O, fully unit-testable). Each NIGHT is priced on its own, so a stay
 * that crosses into high season is charged correctly per night:
 *
 * <pre>night = base(roomType) x season(date) x occupancy(stay)</pre>
 */
@Component
public class PriceCalculator {

  public static final long NEUTRAL = Money.BASIS_POINTS;

  private final PricingProperties props;

  public PriceCalculator(PricingProperties props) {
    this.props = props;
  }

  public List<NightPrice> nights(
      String roomType, LocalDate checkIn, LocalDate checkOut, int occupancyPercent) {
    Long base = props.basePrices().get(roomType);
    if (base == null) {
      throw new BadRequestException("Unknown room type: " + roomType);
    }
    long occupancyBp = occupancyMultiplier(occupancyPercent);
    List<NightPrice> result = new ArrayList<>();
    for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
      long seasonBp = seasonMultiplier(night);
      Money price =
          Money.of(base, props.baseCurrency()).applyBasisPoints(seasonBp).applyBasisPoints(occupancyBp);
      result.add(new NightPrice(night, price, seasonBp, occupancyBp));
    }
    return result;
  }

  long seasonMultiplier(LocalDate date) {
    MonthDay day = MonthDay.from(date);
    return props.seasons().stream()
        .filter(s -> contains(s, day))
        .mapToLong(Season::multiplierBasisPoints)
        .max()
        .orElse(NEUTRAL);
  }

  long occupancyMultiplier(int occupancyPercent) {
    return props.occupancyTiers().stream()
        .filter(t -> occupancyPercent >= t.minPercent())
        .max(Comparator.comparingInt(OccupancyTier::minPercent))
        .map(OccupancyTier::multiplierBasisPoints)
        .orElse(NEUTRAL);
  }

  private static boolean contains(Season s, MonthDay day) {
    MonthDay from = MonthDay.parse("--" + s.from());
    MonthDay to = MonthDay.parse("--" + s.to());
    if (!from.isAfter(to)) {
      return !day.isBefore(from) && !day.isAfter(to);
    }
    return !day.isBefore(from) || !day.isAfter(to); // wraps the year end
  }

  public record NightPrice(
      LocalDate date, Money price, long seasonMultiplierBp, long occupancyMultiplierBp) {}
}
