package com.grandhotel.pricing.fx;

import com.grandhotel.common.context.ContextPropagatingInterceptor;
import com.grandhotel.common.exception.ServiceUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls fx-service with short timeouts. Resilience: the last good rate per pair is kept, so when
 * fx-service is down quotes still work, flagged {@code stale=true}. Only with no rate at all do we
 * fail with 503 - we never invent a rate.
 */
@Component
public class FxClient {

  private static final Logger log = LoggerFactory.getLogger(FxClient.class);

  private final RestClient http;
  private final Map<String, FxQuote> lastGood = new ConcurrentHashMap<>();

  public FxClient(
      @Value("${app.clients.fx.url}") String baseUrl,
      @Value("${app.clients.fx.timeout:PT2S}") Duration timeout,
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

  public FxQuote rate(String base, String quote) {
    String pair = base + "/" + quote;
    try {
      FxRateDto dto =
          http.get()
              .uri(u -> u.path("/api/fx/rates/latest").queryParam("base", base).queryParam("quote", quote).build())
              .retrieve()
              .body(FxRateDto.class);
      if (dto == null || dto.rateMicros() <= 0) {
        throw new IllegalStateException("Empty FX response");
      }
      FxQuote fresh = new FxQuote(base, quote, dto.rateMicros(), dto.asOf(), dto.source(), false);
      lastGood.put(pair, fresh);
      return fresh;
    } catch (RestClientException | IllegalStateException e) {
      FxQuote cached = lastGood.get(pair);
      if (cached == null) {
        throw new ServiceUnavailableException("Exchange rates are unavailable", e);
      }
      log.warn("fx-service unavailable, using last known {} rate from {}", pair, cached.asOf());
      return cached.asStale();
    }
  }

  record FxRateDto(String base, String quote, long rateMicros, Instant asOf, String source) {}

  public record FxQuote(
      String base, String quote, long rateMicros, Instant asOf, String source, boolean stale) {

    FxQuote asStale() {
      return new FxQuote(base, quote, rateMicros, asOf, source, true);
    }
  }
}
