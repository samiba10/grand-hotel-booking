package com.grandhotel.booking.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * AuditLog (Id, Action, EntityType, EntityId, UserId, Timestamp) from the assignment, plus the
 * request context: transaction id/type, session id, HTTP outcome. Append-only.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String action;

  @Column(name = "entity_type", nullable = false, length = 50)
  private String entityType;

  @Column(name = "entity_id", nullable = false, length = 120)
  private String entityId;

  @Column(name = "user_id", nullable = false, length = 50)
  private String userId;

  @Column(nullable = false)
  private Instant timestamp;

  @Column(name = "transaction_id", length = 100)
  private String transactionId;

  @Column(name = "transaction_type", length = 50)
  private String transactionType;

  @Column(name = "session_id", length = 100)
  private String sessionId;

  private Integer outcome;

  @Column(length = 500)
  private String details;

  protected AuditLog() {}

  AuditLog(
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
    this.action = action;
    this.entityType = entityType;
    this.entityId = truncate(entityId, 120);
    this.userId = userId;
    this.timestamp = timestamp;
    this.transactionId = transactionId;
    this.transactionType = transactionType;
    this.sessionId = sessionId;
    this.outcome = outcome;
    this.details = truncate(details, 500);
  }

  private static String truncate(String s, int max) {
    return s == null || s.length() <= max ? s : s.substring(0, max);
  }

  public Long getId() {
    return id;
  }

  public String getAction() {
    return action;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getUserId() {
    return userId;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public String getSessionId() {
    return sessionId;
  }

  public Integer getOutcome() {
    return outcome;
  }

  public String getDetails() {
    return details;
  }
}
