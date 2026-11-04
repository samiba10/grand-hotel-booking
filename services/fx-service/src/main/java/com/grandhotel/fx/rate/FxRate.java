package com.grandhotel.fx.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One observed rate. Append-only: every refresh inserts a row, so the history is kept and a quote
 * can always say which rate (and from when) it used.
 */
@Entity
@Table(name = "fx_rates")
public class FxRate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "base_currency", nullable = false, length = 3)
  private String baseCurrency;

  @Column(name = "quote_currency", nullable = false, length = 3)
  private String quoteCurrency;

  /** Units of quote currency per 1 unit of base, x 1_000_000. BIGINT, no floating point. */
  @Column(name = "rate_micros", nullable = false)
  private long rateMicros;

  @Column(name = "as_of", nullable = false)
  private Instant asOf;

  @Column(nullable = false, length = 40)
  private String source;

  protected FxRate() {}

  public FxRate(String base, String quote, long rateMicros, Instant asOf, String source) {
    this.baseCurrency = base;
    this.quoteCurrency = quote;
    this.rateMicros = rateMicros;
    this.asOf = asOf;
    this.source = source;
  }

  public String getBaseCurrency() {
    return baseCurrency;
  }

  public String getQuoteCurrency() {
    return quoteCurrency;
  }

  public long getRateMicros() {
    return rateMicros;
  }

  public Instant getAsOf() {
    return asOf;
  }

  public String getSource() {
    return source;
  }
}
