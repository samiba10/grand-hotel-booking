package com.grandhotel.common.exception;

/** A downstream dependency (pricing, FX) failed or timed out. Mapped to HTTP 503. */
public class ServiceUnavailableException extends RuntimeException {

  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
