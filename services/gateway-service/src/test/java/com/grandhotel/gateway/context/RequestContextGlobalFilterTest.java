package com.grandhotel.gateway.context;

import static com.grandhotel.gateway.TestFixtures.PROPS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class RequestContextGlobalFilterTest {

  private final RequestContextGlobalFilter filter =
      new RequestContextGlobalFilter(new TransactionTypeResolver(PROPS));
  private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
  private final GatewayFilterChain sink =
      exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
      };

  @Test
  void everyRequestGetsATypedTransactionIdDownstreamAndInTheResponse() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/bookings"));
    filter.filter(exchange, sink).block();

    var down = forwarded.get().getRequest().getHeaders();
    assertThat(down.getFirst("X-Transaction-Id")).startsWith("BOOKING_CREATE-");
    assertThat(down.getFirst("X-Transaction-Type")).isEqualTo("BOOKING_CREATE");
    assertThat(exchange.getResponse().getHeaders().getFirst("X-Transaction-Id"))
        .isEqualTo(down.getFirst("X-Transaction-Id"));
  }

  @Test
  void clientCannotForgeTheTransactionId() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/bookings/1").header("X-Transaction-Id", "FAKE-123456789"));
    filter.filter(exchange, sink).block();
    assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Transaction-Id"))
        .doesNotContain("FAKE")
        .startsWith("HTTP_GET-");
  }

  @Test
  void sessionIdPingPong() {
    var first = MockServerWebExchange.from(MockServerHttpRequest.get("/api/rooms/availability"));
    filter.filter(first, sink).block();
    String issued = first.getResponse().getHeaders().getFirst("X-Session-Id");
    assertThat(issued).isNotBlank();

    var second =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/rooms/availability").header("X-Session-Id", issued));
    filter.filter(second, sink).block();
    assertThat(second.getResponse().getHeaders().getFirst("X-Session-Id")).isEqualTo(issued);
  }

  @Test
  void maliciousSessionIdIsReplaced() {
    var exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/rooms/availability")
                .header("X-Session-Id", "x\nFAKE LOG LINE"));
    filter.filter(exchange, sink).block();
    assertThat(exchange.getResponse().getHeaders().getFirst("X-Session-Id")).doesNotContain("FAKE");
  }
}
