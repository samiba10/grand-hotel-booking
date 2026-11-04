package com.grandhotel.common.exception;

/** Authenticated, but not allowed on this resource (AuthZ). Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }
}
