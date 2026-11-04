package com.grandhotel.booking.pricing;

import com.grandhotel.booking.booking.StayDates;
import com.grandhotel.booking.room.RoomType;
import com.grandhotel.common.context.ContextPropagatingInterceptor;
import com.grandhotel.common.exception.ServiceUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls pricing-service, relaying the caller's JWT and the audit context. One call prices all room
 * types of a search. Failure = 503 for the client: we never fall back to an invented price.
 */
@Component
public class PricingClient {

  private final RestClient http;

  public PricingClient(
      @Value("${app.clients.pricing.url}") String baseUrl,
      @Value("${app.clients.pricing.timeout:PT3S}") Duration timeout,
      ContextPropagatingInterceptor propagation) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(timeout);
    factory.setReadTimeout(timeout);
    this.http =
        RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .requestInterceptor(propagation)
            .build();
  }

  public PricingQuote quote(
      StayDates dates, int occupancyPercent, Set<RoomType> roomTypes, String currency) {
    var request =
        new QuoteRequest(
            dates.checkIn(),
            dates.checkOut(),
            occupancyPercent,
            roomTypes.stream().map(Enum::name).toList(),
            currency);
    try {
      QuoteResponse res =
          http.post().uri("/api/pricing/quotes").body(request).retrieve().body(QuoteResponse.class);
      if (res == null) {
        throw new IllegalStateException("Empty pricing response");
      }
      Map<RoomType, Long> totals = new EnumMap<>(RoomType.class);
      res.quotes().forEach(q -> totals.put(RoomType.valueOf(q.roomType()), q.totalMinor()));
      return new PricingQuote(
          res.currency(), totals, res.fx().rateMicros(), res.fx().asOf(), res.fx().stale());
    } catch (RestClientException | IllegalStateException e) {
      throw new ServiceUnavailableException("Pricing is temporarily unavailable", e);
    }
  }

  record QuoteRequest(
      LocalDate checkIn,
      LocalDate checkOut,
      int occupancyPercent,
      List<String> roomTypes,
      String currency) {}

  record QuoteResponse(String currency, FxInfo fx, List<RoomTypeQuote> quotes) {}

  record FxInfo(String base, String quote, long rateMicros, Instant asOf, boolean stale) {}

  record RoomTypeQuote(String roomType, String currency, long totalMinor, List<Night> nights) {}

  record Night(
      LocalDate date, long amountMinor, long seasonMultiplierBp, long occupancyMultiplierBp) {}
}
