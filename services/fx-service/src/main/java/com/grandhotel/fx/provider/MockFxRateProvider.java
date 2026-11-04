package com.grandhotel.fx.provider;

import com.grandhotel.fx.FxProperties;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simulated market: a bounded random walk around the last stored rate. There is deliberately NO
 * rate constant in code - the starting point is data (Flyway seed), and it moves over time.
 */
@Component
public class MockFxRateProvider implements FxRateProvider {

  private final int maxStepBasisPoints;
  private final RandomGenerator random;

  @Autowired
  public MockFxRateProvider(FxProperties props) {
    this(props.maxStepBasisPoints(), new SplittableRandom());
  }

  MockFxRateProvider(int maxStepBasisPoints, RandomGenerator random) {
    this.maxStepBasisPoints = maxStepBasisPoints;
    this.random = random;
  }

  @Override
  public long fetchRateMicros(String base, String quote, long lastKnownMicros) {
    long stepBp = random.nextLong(-maxStepBasisPoints, maxStepBasisPoints + 1L);
    long next = lastKnownMicros + Math.multiplyExact(lastKnownMicros, stepBp) / 10_000L;
    return Math.max(1, next);
  }

  @Override
  public String name() {
    return "mock-market";
  }
}
