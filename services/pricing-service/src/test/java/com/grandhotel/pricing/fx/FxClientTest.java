package com.grandhotel.pricing.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grandhotel.common.context.ContextPropagatingInterceptor;
import com.grandhotel.common.exception.ServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Resilience of the FX call against a real (local) HTTP server: fresh, stale fallback, 503. */
class FxClientTest {

  private static final byte[] RATE =
      """
      {"base":"ILS","quote":"USD","rateMicros":270270,"asOf":"2026-01-01T00:00:00Z","source":"test"}
      """
          .getBytes(StandardCharsets.UTF_8);

  private final AtomicInteger status = new AtomicInteger(200);
  private HttpServer server;
  private FxClient client;

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/fx/rates/latest",
        exchange -> {
          int code = status.get();
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          if (code == 200) {
            exchange.sendResponseHeaders(200, RATE.length);
            exchange.getResponseBody().write(RATE);
          } else {
            exchange.sendResponseHeaders(code, -1);
          }
          exchange.close();
        });
    server.start();
    client =
        new FxClient(
            "http://127.0.0.1:" + server.getAddress().getPort(),
            Duration.ofSeconds(1),
            new ContextPropagatingInterceptor());
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @Test
  void freshRateIsNotStale() {
    var quote = client.rate("ILS", "USD");
    assertThat(quote.rateMicros()).isEqualTo(270_270);
    assertThat(quote.stale()).isFalse();
  }

  @Test
  void outageFallsBackToLastKnownRateFlaggedStale() {
    client.rate("ILS", "USD");
    status.set(500);

    var quote = client.rate("ILS", "USD");

    assertThat(quote.rateMicros()).isEqualTo(270_270);
    assertThat(quote.stale()).isTrue();
  }

  @Test
  void outageWithoutAnyKnownRateIs503NeverAnInventedRate() {
    status.set(503);
    assertThatThrownBy(() -> client.rate("ILS", "USD"))
        .isInstanceOf(ServiceUnavailableException.class);
  }
}
