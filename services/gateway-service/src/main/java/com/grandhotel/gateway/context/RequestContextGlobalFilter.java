package com.grandhotel.gateway.context;

import com.grandhotel.common.context.RequestContext;
import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.gateway.FilterOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Creates the audit context of every request at the edge:
 *
 * <ul>
 *   <li>a NEW transaction id, typed (e.g. {@code BOOKING_CREATE-<uuid>}). Any id sent by the client
 *       is overwritten - clients cannot forge or reuse transaction ids;
 *   <li>the session id, ping-pong: kept if the client echoed a valid one, issued otherwise.
 * </ul>
 *
 * Both go downstream as headers and back to the client in the response, and one ACCESS log line is
 * written per request (tx, type, session, user, status, latency).
 */
@Component
public class RequestContextGlobalFilter implements GlobalFilter, Ordered {

  public static final String TX_ID_ATTR = "gh.transactionId";
  public static final String SESSION_ATTR = "gh.sessionId";
  public static final String USER_ATTR = "gh.user";

  private static final Logger access = LoggerFactory.getLogger("ACCESS");

  private final TransactionTypeResolver types;

  public RequestContextGlobalFilter(TransactionTypeResolver types) {
    this.types = types;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    long started = System.nanoTime();
    ServerHttpRequest request = exchange.getRequest();
    String type = types.resolve(request.getMethod(), request.getPath().pathWithinApplication());
    String txId = RequestContext.newTransactionId(type);
    String clientSession = request.getHeaders().getFirst(RequestContext.SESSION_ID_HEADER);
    String sessionId =
        RequestContext.isSafeId(clientSession) ? clientSession : RequestContext.newSessionId();

    ServerHttpRequest withContext =
        request
            .mutate()
            .headers(
                h -> {
                  h.set(RequestContext.TRANSACTION_ID_HEADER, txId);
                  h.set(RequestContext.TRANSACTION_TYPE_HEADER, type);
                  h.set(RequestContext.SESSION_ID_HEADER, sessionId);
                })
            .build();
    exchange.getAttributes().put(TX_ID_ATTR, txId);
    exchange.getAttributes().put(SESSION_ATTR, sessionId);
    exchange.getResponse().getHeaders().set(RequestContext.TRANSACTION_ID_HEADER, txId);
    exchange.getResponse().getHeaders().set(RequestContext.SESSION_ID_HEADER, sessionId);

    return chain
        .filter(exchange.mutate().request(withContext).build())
        .doFinally(signal -> logAccess(exchange, request, type, txId, sessionId, started));
  }

  private static void logAccess(
      ServerWebExchange exchange,
      ServerHttpRequest request,
      String type,
      String txId,
      String sessionId,
      long started) {
    HttpStatusCode status = exchange.getResponse().getStatusCode();
    Object user = exchange.getAttribute(USER_ATTR);
    access.info(
        "tx={} type={} sid={} user={} method={} path={} status={} durationMs={}",
        txId,
        type,
        sessionId,
        user instanceof AuthenticatedUser u ? u.username() : "-",
        request.getMethod(),
        request.getPath().value(),
        status == null ? "-" : status.value(),
        (System.nanoTime() - started) / 1_000_000);
  }

  @Override
  public int getOrder() {
    return FilterOrder.REQUEST_CONTEXT;
  }
}
