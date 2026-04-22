package com.mining.soft.application.port.in;
import com.mining.soft.domain.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
public interface GetAuditLogsUseCase {
    List<AuditLog> getAuditLogs(Long userId, String action, LocalDateTime startDate, LocalDateTime endDate);
}
