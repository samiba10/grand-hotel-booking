package com.grandhotel.common.money;

import java.util.regex.Pattern;

/**
 * Money as an integer number of minor units (cents / agorot): {@code 1050 ILS} = 10.50 ILS.
 * Persisted as BIGINT. No floating point anywhere: multipliers are basis points (10000 = x1.0)
 * and FX rates are micros (1_000_000 = 1.0). All arithmetic is overflow-checked.
 */
public record Money(long amountMinor, String currency) {

  private static final Pattern ISO_4217 = Pattern.compile("^[A-Z]{3}$");
  public static final long BASIS_POINTS = 10_000L;
  public static final long MICROS = 1_000_000L;

  public Money {
    if (currency == null || !ISO_4217.matcher(currency).matches()) {
      throw new IllegalArgumentException("Currency must be an ISO-4217 code, got: " + currency);
    }
  }

  public static Money of(long amountMinor, String currency) {
    return new Money(amountMinor, currency);
  }

  public static Money zero(String currency) {
    return new Money(0, currency);
  }

  public Money plus(Money other) {
    requireSameCurrency(other);
    return new Money(Math.addExact(amountMinor, other.amountMinor), currency);
  }

  public Money times(long factor) {
    return new Money(Math.multiplyExact(amountMinor, factor), currency);
  }

  /** Multiplies by {@code basisPoints / 10000}, rounding half away from zero. */
  public Money applyBasisPoints(long basisPoints) {
    return new Money(
        roundDiv(Math.multiplyExact(amountMinor, basisPoints), BASIS_POINTS), currency);
  }

  /** Converts using a rate expressed in micros of {@code target} per 1 unit of this currency. */
  public Money convert(String target, long rateMicros) {
    if (rateMicros <= 0) {
      throw new IllegalArgumentException("FX rate must be positive");
    }
    if (target.equals(currency)) {
      return this;
    }
    return new Money(roundDiv(Math.multiplyExact(amountMinor, rateMicros), MICROS), target);
  }

  /** Human readable, e.g. {@code 10.50 USD}. For display only - never parse it back. */
  public String format() {
    long abs = Math.abs(amountMinor);
    return (amountMinor < 0 ? "-" : "") + abs / 100 + "." + String.format("%02d", abs % 100) + " "
        + currency;
  }

  static long roundDiv(long numerator, long denominator) {
    long quotient = numerator / denominator;
    long remainder = numerator % denominator;
    if (Math.abs(remainder) * 2 >= denominator) {
      quotient += Long.signum(numerator);
    }
    return quotient;
  }

  private void requireSameCurrency(Money other) {
    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
    }
  }
}
