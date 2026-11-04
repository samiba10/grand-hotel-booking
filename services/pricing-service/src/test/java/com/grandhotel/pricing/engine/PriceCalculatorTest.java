package com.grandhotel.pricing.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grandhotel.common.exception.BadRequestException;
import com.grandhotel.pricing.engine.PricingProperties.OccupancyTier;
import com.grandhotel.pricing.engine.PricingProperties.Season;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

  private final PriceCalculator calc =
      new PriceCalculator(
          new PricingProperties(
              "ILS",
              Map.of("STANDARD", 10_000L),
              List.of(
                  new Season("summer", "07-01", "08-31", 13_000),
                  new Season("holidays", "12-20", "01-05", 12_500)),
              List.of(new OccupancyTier(0, 10_000), new OccupancyTier(80, 12_500))));

  @Test
  void offSeasonLowOccupancyIsBasePrice() {
    var nights = calc.nights("STANDARD", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3), 10);
    assertThat(nights).hasSize(2).allMatch(n -> n.price().amountMinor() == 10_000);
  }

  @Test
  void eachNightIsPricedByItsOwnSeason() {
    // 30 Jun (off) + 1 Jul (summer)
    var nights = calc.nights("STANDARD", LocalDate.of(2027, 6, 30), LocalDate.of(2027, 7, 2), 0);
    assertThat(nights.get(0).price().amountMinor()).isEqualTo(10_000);
    assertThat(nights.get(1).price().amountMinor()).isEqualTo(13_000);
  }

  @Test
  void seasonWrappingTheYearEndMatchesBothSides() {
    assertThat(calc.seasonMultiplier(LocalDate.of(2027, 12, 31))).isEqualTo(12_500);
    assertThat(calc.seasonMultiplier(LocalDate.of(2028, 1, 3))).isEqualTo(12_500);
    assertThat(calc.seasonMultiplier(LocalDate.of(2028, 1, 6))).isEqualTo(10_000);
  }

  @Test
  void occupancyAndSeasonCompound() {
    // 100.00 x 1.30 x 1.25 = 162.50
    var nights = calc.nights("STANDARD", LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 11), 85);
    assertThat(nights.get(0).price().amountMinor()).isEqualTo(16_250);
  }

  @Test
  void unknownRoomTypeIsRejected() {
    assertThatThrownBy(
            () -> calc.nights("PENTHOUSE", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2), 0))
        .isInstanceOf(BadRequestException.class);
  }
}
