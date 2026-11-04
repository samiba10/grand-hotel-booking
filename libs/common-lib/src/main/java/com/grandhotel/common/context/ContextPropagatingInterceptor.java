package com.grandhotel.common.context;

import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service-to-service calls (RestClient) forward the audit context AND the caller's JWT (token
 * relay). The downstream service re-authenticates and re-authorizes the same user: zero trust,
 * and the same transaction id appears in the logs of every service involved.
 */
public class ContextPropagatingInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    HttpHeaders headers = request.getHeaders();
    copy(headers, RequestContext.TRANSACTION_ID_HEADER, MDC.get(RequestContext.MDC_TX_ID));
    copy(headers, RequestContext.TRANSACTION_TYPE_HEADER, MDC.get(RequestContext.MDC_TX_TYPE));
    copy(headers, RequestContext.SESSION_ID_HEADER, MDC.get(RequestContext.MDC_SESSION_ID));
    if (headers.getFirst(HttpHeaders.AUTHORIZATION) == null) {
      copy(headers, HttpHeaders.AUTHORIZATION, currentAuthorization());
    }
    return execution.execute(request, body);
  }

  private static void copy(HttpHeaders headers, String name, String value) {
    if (value != null && !value.isBlank()) {
      headers.set(name, value);
    }
  }

  private static String currentAuthorization() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      return attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
    return null;
  }
}
