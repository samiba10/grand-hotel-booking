package com.grandhotel.fx.rate;

import com.grandhotel.common.exception.NotFoundException;
import com.grandhotel.common.money.Money;
import com.grandhotel.fx.FxProperties;
import com.grandhotel.fx.provider.FxRateProvider;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FxService {

  private static final Logger log = LoggerFactory.getLogger(FxService.class);

  private final FxRateRepository rates;
  private final FxRateProvider provider;
  private final FxProperties props;
  private final Clock clock;

  public FxService(
      FxRateRepository rates, FxRateProvider provider, FxProperties props, Clock clock) {
    this.rates = rates;
    this.provider = provider;
    this.props = props;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public FxRate latest(String base, String quote) {
    if (base.equals(quote)) {
      return new FxRate(base, quote, Money.MICROS, clock.instant(), "identity");
    }
    return rates
        .findFirstByBaseCurrencyAndQuoteCurrencyOrderByAsOfDescIdDesc(base, quote)
        .orElseThrow(() -> new NotFoundException("FX rate", base + "/" + quote));
  }

  /** Pulls a fresh rate for every configured pair and stores it with its inverse. */
  @Scheduled(
      initialDelayString = "${app.fx.refresh-interval:PT30S}",
      fixedDelayString = "${app.fx.refresh-interval:PT30S}")
  @Transactional
  public void refresh() {
    for (String pair : props.pairs()) {
      String[] parts = pair.split("/");
      String base = parts[0];
      String quote = parts[1];
      rates
          .findFirstByBaseCurrencyAndQuoteCurrencyOrderByAsOfDescIdDesc(base, quote)
          .ifPresent(last -> store(base, quote, last.getRateMicros()));
    }
  }

  private void store(String base, String quote, long lastMicros) {
    long next = provider.fetchRateMicros(base, quote, lastMicros);
    long inverse = Math.max(1, (Money.MICROS * Money.MICROS + next / 2) / next);
    var now = clock.instant();
    rates.save(new FxRate(base, quote, next, now, provider.name()));
    rates.save(new FxRate(quote, base, inverse, now, provider.name()));
    log.info("FX {}/{} {} -> {} micros", base, quote, lastMicros, next);
  }
}
