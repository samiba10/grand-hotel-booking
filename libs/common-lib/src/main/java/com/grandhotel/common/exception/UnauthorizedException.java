package com.grandhotel.common.exception;

/** Wrong credentials. Mapped to HTTP 401. */
public class UnauthorizedException extends RuntimeException {

  public UnauthorizedException(String message) {
    super(message);
  }
}
