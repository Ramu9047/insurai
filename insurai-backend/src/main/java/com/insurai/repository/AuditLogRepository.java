package com.insurai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.insurai.model.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPerformedBy(Long performedBy);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByPerformedByRole(String role);

    List<AuditLog> findBySeverity(String severity);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = ?1 AND a.entityId = ?2 ORDER BY a.timestamp DESC")
    List<AuditLog> findEntityHistory(String entityType, Long entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.performedByRole IN ('SUPER_ADMIN', 'COMPANY_ADMIN') ORDER BY a.timestamp DESC")
    List<AuditLog> findAdminActions();

    @Query("SELECT a FROM AuditLog a WHERE a.severity = 'CRITICAL' ORDER BY a.timestamp DESC")
    List<AuditLog> findCriticalActions();

    @Query("SELECT a FROM AuditLog a, User u WHERE a.performedBy = u.id AND u.company.id = :companyId ORDER BY a.timestamp DESC")
    List<AuditLog> findByCompanyId(Long companyId);
}
