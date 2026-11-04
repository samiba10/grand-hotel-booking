package com.grandhotel.common.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First filter in every servlet service: puts the transaction / session ids into the logging MDC
 * (so EVERY log line carries them) and echoes them in the response. Runs before Spring Security,
 * so even a 401 is traceable.
 *
 * <p>Ids normally come from the gateway. A direct call (bypassing the gateway) still gets fresh
 * ids, typed {@code DIRECT}, so nothing is ever unaudited.
 */
// Not named RequestContextFilter: Spring MVC already registers a bean called "requestContextFilter".
public class AuditContextFilter extends OncePerRequestFilter implements Ordered {

  static final String DIRECT = "DIRECT";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String type = request.getHeader(RequestContext.TRANSACTION_TYPE_HEADER);
    if (!RequestContext.isSafeType(type)) {
      type = DIRECT;
    }
    String txId = request.getHeader(RequestContext.TRANSACTION_ID_HEADER);
    if (!RequestContext.isSafeId(txId)) {
      txId = RequestContext.newTransactionId(type);
    }
    String sessionId = request.getHeader(RequestContext.SESSION_ID_HEADER);
    if (!RequestContext.isSafeId(sessionId)) {
      sessionId = RequestContext.newSessionId();
    }

    MDC.put(RequestContext.MDC_TX_ID, txId);
    MDC.put(RequestContext.MDC_TX_TYPE, type);
    MDC.put(RequestContext.MDC_SESSION_ID, sessionId);
    response.setHeader(RequestContext.TRANSACTION_ID_HEADER, txId);
    response.setHeader(RequestContext.SESSION_ID_HEADER, sessionId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 5;
  }
}
