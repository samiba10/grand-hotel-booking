package com.grandhotel.common.web;

import com.grandhotel.common.context.RequestContext;
import java.time.Instant;
import java.util.List;

/**
 * The single error contract of every service and of the gateway. {@code transactionId} lets a user
 * quote a reference to support, and ops find every log line of that request.
 */
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details,
    String transactionId) {

  public static ApiError of(int status, String error, String message, String path) {
    return of(status, error, message, path, List.of());
  }

  public static ApiError of(
      int status, String error, String message, String path, List<String> details) {
    return new ApiError(
        Instant.now(), status, error, message, path, details, RequestContext.transactionId());
  }

  /** For reactive code (the gateway), where there is no MDC. */
  public static ApiError withTransaction(
      int status, String error, String message, String path, String transactionId) {
    return new ApiError(Instant.now(), status, error, message, path, List.of(), transactionId);
  }
}
