package com.grandhotel.gateway;

import org.springframework.core.Ordered;

/**
 * The edge pipeline, in one place:
 *
 * <ol>
 *   <li>request context - transaction + session ids (so even a rejected request is traceable)
 *   <li>rate limit - cheapest rejection, before any crypto
 *   <li>AuthN - is the JWT valid? who is it?
 *   <li>AuthZ - may this identity call this route at all?
 *   <li>routing to the microservice (Spring Cloud Gateway)
 * </ol>
 */
public final class FilterOrder {

  public static final int REQUEST_CONTEXT = Ordered.HIGHEST_PRECEDENCE;
  public static final int RATE_LIMIT = REQUEST_CONTEXT + 10;
  public static final int AUTHENTICATION = RATE_LIMIT + 10;
  public static final int AUTHORIZATION = AUTHENTICATION + 10;

  private FilterOrder() {}
}
