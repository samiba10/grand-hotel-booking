package com.grandhotel.booking.audit;

import com.grandhotel.common.context.RequestContext;
import com.grandhotel.common.security.AuthenticatedUser;
import java.time.Clock;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes audit rows, stamped with who / which transaction / which session. */
@Service
public class AuditService {

  static final String REQUEST = "HTTP_REQUEST";

  private final AuditRepository repo;
  private final Clock clock;

  public AuditService(AuditRepository repo, Clock clock) {
    this.repo = repo;
    this.clock = clock;
  }

  /**
   * Business event (BOOKING_CREATED, ...). Joins the caller's transaction: the event is stored if
   * and only if the change itself is committed.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(String action, String entityType, Object entityId, String details) {
    repo.save(entry(action, entityType, String.valueOf(entityId), null, details));
  }

  /** Access audit of every API request, in its own transaction (also written for failures). */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordRequest(String method, String path, int status) {
    String action = RequestContext.transactionType() == null ? REQUEST : RequestContext.transactionType();
    repo.save(entry(action, REQUEST, method + " " + path, status, null));
  }

  private AuditLog entry(
      String action, String entityType, String entityId, Integer outcome, String details) {
    return new AuditLog(
        action,
        entityType,
        entityId,
        currentUser(),
        clock.instant(),
        RequestContext.transactionId(),
        RequestContext.transactionType(),
        RequestContext.sessionId(),
        outcome,
        details);
  }

  private static String currentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
      return user.username();
    }
    return "anonymous";
  }
}
