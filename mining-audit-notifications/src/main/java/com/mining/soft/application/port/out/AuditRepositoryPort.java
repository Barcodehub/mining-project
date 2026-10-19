package com.mining.soft.application.port.out;
import com.mining.soft.domain.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
public interface AuditRepositoryPort {
    AuditLog save(AuditLog auditLog);
    boolean exists(String entity, Long entityId, String action);
    List<AuditLog> findByFilters(Long userId, String action, LocalDateTime startDate, LocalDateTime endDate);
}
