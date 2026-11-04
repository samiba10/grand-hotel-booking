package com.grandhotel.booking.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit")
public class AuditController {

  private final AuditRepository repo;

  public AuditController(AuditRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('audit:read')")
  @Transactional(readOnly = true)
  @Operation(summary = "Audit trail: by entity, by transaction id, by session id, or latest")
  public List<AuditEntry> search(
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String entityId,
      @RequestParam(required = false) String transactionId,
      @RequestParam(required = false) String sessionId,
      @RequestParam(defaultValue = "50") int limit) {
    var page = PageRequest.of(0, Math.clamp(limit, 1, 200));
    List<AuditLog> rows;
    if (transactionId != null) {
      rows = repo.findByTransactionIdOrderByIdAsc(transactionId);
    } else if (sessionId != null) {
      rows = repo.findBySessionIdOrderByIdDesc(sessionId, page);
    } else if (entityType != null && entityId != null) {
      rows = repo.findByEntityTypeAndEntityIdOrderByTimestampDescIdDesc(entityType, entityId, page);
    } else {
      rows = repo.findAllByOrderByIdDesc(page);
    }
    return rows.stream().map(AuditEntry::from).toList();
  }

  public record AuditEntry(
      Long id,
      String action,
      String entityType,
      String entityId,
      String userId,
      Instant timestamp,
      String transactionId,
      String transactionType,
      String sessionId,
      Integer outcome,
      String details) {

    static AuditEntry from(AuditLog a) {
      return new AuditEntry(
          a.getId(),
          a.getAction(),
          a.getEntityType(),
          a.getEntityId(),
          a.getUserId(),
          a.getTimestamp(),
          a.getTransactionId(),
          a.getTransactionType(),
          a.getSessionId(),
          a.getOutcome(),
          a.getDetails());
    }
  }
}
