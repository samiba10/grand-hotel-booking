package com.grandhotel.gateway.web;

import com.grandhotel.common.web.ApiError;
import com.grandhotel.gateway.context.RequestContextGlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/** Writes the shared {@link ApiError} contract (with transaction id) from reactive filters. */
@Component
public class ErrorResponseWriter {

  private final JsonMapper mapper;

  public ErrorResponseWriter(JsonMapper mapper) {
    this.mapper = mapper;
  }

  public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    ApiError body =
        ApiError.withTransaction(
            status.value(),
            status.getReasonPhrase(),
            message,
            exchange.getRequest().getPath().value(),
            exchange.getAttribute(RequestContextGlobalFilter.TX_ID_ATTR));
    DataBuffer buffer = response.bufferFactory().wrap(mapper.writeValueAsBytes(body));
    return response.writeWith(Mono.just(buffer));
  }
}
