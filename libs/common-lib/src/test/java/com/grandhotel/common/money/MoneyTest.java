package com.grandhotel.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void storesMinorUnits() {
    assertThat(Money.of(1002, "USD").format()).isEqualTo("10.02 USD");
    assertThat(Money.of(1050, "USD").format()).isEqualTo("10.50 USD");
  }

  @Test
  void basisPointsRoundHalfAwayFromZero() {
    // 101 * 1.125 = 113.625 -> 114
    assertThat(Money.of(101, "ILS").applyBasisPoints(11_250).amountMinor()).isEqualTo(114);
    // 100 * 1.1 = 110 exactly
    assertThat(Money.of(100, "ILS").applyBasisPoints(11_000).amountMinor()).isEqualTo(110);
  }

  @Test
  void convertsWithIntegerRate() {
    // 370.00 ILS at 0.270270 USD/ILS = 100.00 USD
    assertThat(Money.of(37_000, "ILS").convert("USD", 270_270)).isEqualTo(Money.of(10_000, "USD"));
  }

  @Test
  void sameCurrencyConversionIsIdentity() {
    assertThat(Money.of(500, "USD").convert("USD", 1_000_000)).isEqualTo(Money.of(500, "USD"));
  }

  @Test
  void rejectsMixedCurrencies() {
    assertThatThrownBy(() -> Money.of(1, "USD").plus(Money.of(1, "ILS")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void overflowIsDetectedNotWrapped() {
    assertThatThrownBy(() -> Money.of(Long.MAX_VALUE, "USD").times(2))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void rejectsInvalidCurrency() {
    assertThatThrownBy(() -> Money.of(1, "usd")).isInstanceOf(IllegalArgumentException.class);
  }
}
