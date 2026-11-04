package com.grandhotel.booking.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Persists one AuditLog row per API request - reads included. Never breaks the request. */
@Component
public class AuditInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);

  private final AuditService audit;

  public AuditInterceptor(AuditService audit) {
    this.audit = audit;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {
      audit.recordRequest(request.getMethod(), request.getRequestURI(), response.getStatus());
    } catch (RuntimeException e) {
      log.error("Could not write request audit", e);
    }
  }
}
