package com.grandhotel.common.context;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/**
 * Audit context carried by every request, end to end (FE -> gateway -> services -> logs -> AuditLog).
 *
 * <ul>
 *   <li><b>Transaction id</b> - new for EVERY request, generated at the edge, prefixed with the
 *       request type, e.g. {@code BOOKING_CREATE-3f2a...}. One id = one user action across all
 *       services.
 *   <li><b>Session id</b> - "ping-pong": issued by the gateway on first contact, returned in every
 *       response, echoed back by the client on every request. Sticky to the browser, so a user's
 *       whole journey (before and after login) can be followed.
 * </ul>
 */
public final class RequestContext {

  public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";
  public static final String TRANSACTION_TYPE_HEADER = "X-Transaction-Type";
  public static final String SESSION_ID_HEADER = "X-Session-Id";

  public static final String MDC_TX_ID = "txId";
  public static final String MDC_TX_TYPE = "txType";
  public static final String MDC_SESSION_ID = "sessionId";
  public static final String MDC_USER = "user";

  /** Header values end up in logs: strict charset prevents log injection. */
  private static final Pattern SAFE_VALUE = Pattern.compile("^[A-Za-z0-9_.:-]{8,100}$");

  private static final Pattern SAFE_TYPE = Pattern.compile("^[A-Z0-9_]{2,50}$");

  private RequestContext() {}

  public static String newTransactionId(String type) {
    return type + "-" + UUID.randomUUID();
  }

  public static String newSessionId() {
    return UUID.randomUUID().toString();
  }

  public static boolean isSafeId(String value) {
    return value != null && SAFE_VALUE.matcher(value).matches();
  }

  public static boolean isSafeType(String value) {
    return value != null && SAFE_TYPE.matcher(value).matches();
  }

  public static String transactionId() {
    return MDC.get(MDC_TX_ID);
  }

  public static String transactionType() {
    return MDC.get(MDC_TX_TYPE);
  }

  public static String sessionId() {
    return MDC.get(MDC_SESSION_ID);
  }
}
