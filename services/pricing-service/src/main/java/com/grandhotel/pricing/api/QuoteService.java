package com.grandhotel.pricing.api;

import com.grandhotel.common.exception.BadRequestException;
import com.grandhotel.common.money.Money;
import com.grandhotel.pricing.api.QuoteDtos.FxInfo;
import com.grandhotel.pricing.api.QuoteDtos.NightlyPrice;
import com.grandhotel.pricing.api.QuoteDtos.QuoteRequest;
import com.grandhotel.pricing.api.QuoteDtos.QuoteResponse;
import com.grandhotel.pricing.api.QuoteDtos.RoomTypeQuote;
import com.grandhotel.pricing.engine.PriceCalculator;
import com.grandhotel.pricing.engine.PricingProperties;
import com.grandhotel.pricing.fx.FxClient;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QuoteService {

  static final int MAX_NIGHTS = 30;

  private final PriceCalculator calculator;
  private final FxClient fx;
  private final PricingProperties props;

  public QuoteService(PriceCalculator calculator, FxClient fx, PricingProperties props) {
    this.calculator = calculator;
    this.fx = fx;
    this.props = props;
  }

  public QuoteResponse quote(QuoteRequest req) {
    long nights = ChronoUnit.DAYS.between(req.checkIn(), req.checkOut());
    if (nights < 1 || nights > MAX_NIGHTS) {
      throw new BadRequestException("Stay must be between 1 and " + MAX_NIGHTS + " nights");
    }
    var rate = fx.rate(props.baseCurrency(), req.currency());

    List<RoomTypeQuote> quotes = new ArrayList<>();
    for (String type : req.roomTypes().stream().sorted().toList()) {
      Money total = Money.zero(req.currency());
      List<NightlyPrice> lines = new ArrayList<>();
      for (var night :
          calculator.nights(type, req.checkIn(), req.checkOut(), req.occupancyPercent())) {
        // Convert per night, total = sum of displayed nights -> lines always add up.
        Money converted = night.price().convert(req.currency(), rate.rateMicros());
        total = total.plus(converted);
        lines.add(
            new NightlyPrice(
                night.date(),
                converted.amountMinor(),
                night.seasonMultiplierBp(),
                night.occupancyMultiplierBp()));
      }
      quotes.add(new RoomTypeQuote(type, req.currency(), total.amountMinor(), lines));
    }
    return new QuoteResponse(
        req.currency(),
        new FxInfo(rate.base(), rate.quote(), rate.rateMicros(), rate.asOf(), rate.stale()),
        quotes);
  }
}
