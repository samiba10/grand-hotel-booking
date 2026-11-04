package com.grandhotel.common.web;

import com.grandhotel.common.exception.BadRequestException;
import com.grandhotel.common.exception.ConflictException;
import com.grandhotel.common.exception.ForbiddenException;
import com.grandhotel.common.exception.NotFoundException;
import com.grandhotel.common.exception.ServiceUnavailableException;
import com.grandhotel.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** One exception-to-HTTP mapping for all servlet services (registered by auto-configuration). */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, e.getMessage(), req, List.of());
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<ApiError> conflict(ConflictException e, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, e.getMessage(), req, List.of());
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<ApiError> badRequest(BadRequestException e, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, e.getMessage(), req, List.of());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiError> unreadable(HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, "Malformed request body", req, List.of());
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ApiError> missingParam(
      MissingServletRequestParameterException e, HttpServletRequest req) {
    return build(
        HttpStatus.BAD_REQUEST, "Missing parameter: " + e.getParameterName(), req, List.of());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ApiError> typeMismatch(
      MethodArgumentTypeMismatchException e, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter: " + e.getName(), req, List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest req) {
    List<String> details =
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .sorted()
            .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", req, details);
  }

  @ExceptionHandler(UnauthorizedException.class)
  ResponseEntity<ApiError> unauthorized(UnauthorizedException e, HttpServletRequest req) {
    return build(HttpStatus.UNAUTHORIZED, e.getMessage(), req, List.of());
  }

  @ExceptionHandler(ForbiddenException.class)
  ResponseEntity<ApiError> forbidden(ForbiddenException e, HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, e.getMessage(), req, List.of());
  }

  /** {@code @PreAuthorize} denials: the caller lacks the permission for this endpoint. */
  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> accessDenied(HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, "You do not have permission for this action", req, List.of());
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  ResponseEntity<ApiError> unavailable(ServiceUnavailableException e, HttpServletRequest req) {
    log.warn("Dependency failure on {}: {}", req.getRequestURI(), e.getMessage(), e.getCause());
    return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), req, List.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest req) {
    // Spring MVC exceptions (405, 404 no handler, 415 ...) carry their own status.
    if (e instanceof ErrorResponse framework) {
      HttpStatus status = HttpStatus.valueOf(framework.getStatusCode().value());
      return build(status, status.getReasonPhrase(), req, List.of());
    }
    log.error("Unhandled error on {} {}", req.getMethod(), req.getRequestURI(), e);
    // Never leak internals (stack traces, SQL) to clients.
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req, List.of());
  }

  private static ResponseEntity<ApiError> build(
      HttpStatus status, String message, HttpServletRequest req, List<String> details) {
    return ResponseEntity.status(status)
        .body(
            ApiError.of(
                status.value(), status.getReasonPhrase(), message, req.getRequestURI(), details));
  }
}
