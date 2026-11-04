package com.grandhotel.fx.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class MockFxRateProviderTest {

  @Test
  void movesWithinTheConfiguredBound() {
    var provider = new MockFxRateProvider(30, new SplittableRandom(42));
    long rate = 270_270;
    for (int i = 0; i < 1_000; i++) {
      long next = provider.fetchRateMicros("ILS", "USD", rate);
      assertThat(Math.abs(next - rate)).isLessThanOrEqualTo(rate * 30 / 10_000);
      assertThat(next).isPositive();
      rate = next;
    }
  }
}
