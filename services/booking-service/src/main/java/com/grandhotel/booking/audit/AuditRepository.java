package com.grandhotel.booking.audit;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditLog, Long> {

  List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDescIdDesc(
      String entityType, String entityId, Pageable page);

  List<AuditLog> findByTransactionIdOrderByIdAsc(String transactionId);

  List<AuditLog> findBySessionIdOrderByIdDesc(String sessionId, Pageable page);

  List<AuditLog> findAllByOrderByIdDesc(Pageable page);
}
