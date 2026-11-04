package com.grandhotel.common.exception;

/** Business-rule violation (room taken, already cancelled, ...). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
