package com.grandhotel.common.exception;

/** Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String resource, Object id) {
    super(resource + " " + id + " not found");
  }
}
